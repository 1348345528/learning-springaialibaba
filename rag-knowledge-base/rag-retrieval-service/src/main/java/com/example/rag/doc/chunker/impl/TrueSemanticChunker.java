package com.example.rag.doc.chunker.impl;

import com.example.rag.doc.chunker.ChunkConfig;
import com.example.rag.doc.chunker.ChunkStrategy;
import com.example.rag.doc.chunker.TextChunk;
import com.example.rag.doc.chunker.config.SemanticChunkConfig;
import com.example.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrueSemanticChunker implements ChunkStrategy {

    private static final String NAME = "true_semantic";
    private static final SemanticChunkConfig DEFAULT_CONFIG = SemanticChunkConfig.defaultConfig();

    private final EmbeddingService embeddingService;

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        return chunk(text, convertConfig(config));
    }

    public List<TextChunk> chunk(String text, SemanticChunkConfig config) {
        if (text == null || text.isEmpty()) return new ArrayList<>();

        SemanticChunkConfig effectiveConfig = config != null ? config : DEFAULT_CONFIG;
        log.debug("开始语义分块，断点检测方法: {}, 阈值: {}",
                effectiveConfig.getBreakpointMethod(), effectiveConfig.getSimilarityThreshold());

        List<String> sentences = splitIntoSentences(text, effectiveConfig);
        log.debug("分割出 {} 个句子", sentences.size());

        if (sentences.size() <= 1) {
            return List.of(TextChunk.builder()
                    .content(text.trim()).index(0).startPos(0).endPos(text.length()).build());
        }

        List<float[]> embeddings = computeEmbeddings(sentences, effectiveConfig);
        log.debug("计算了 {} 个 Embedding 向量", embeddings.size());

        List<Double> similarities = computeSimilarities(embeddings);
        List<Integer> breakpoints = detectBreakpoints(similarities, effectiveConfig);
        List<TextChunk> chunks = createChunks(sentences, breakpoints, effectiveConfig);

        log.info("语义分块完成，生成分块数量: {}", chunks.size());
        return chunks;
    }

    private List<String> splitIntoSentences(String text, SemanticChunkConfig config) {
        String pattern = config.getSentenceSplitPattern();
        boolean keepDelimiter = Boolean.TRUE.equals(config.getKeepSentenceDelimiter());
        Pattern sentencePattern = Pattern.compile(pattern);
        String[] rawSentences = sentencePattern.split(text);

        List<String> sentences = new ArrayList<>();
        if (keepDelimiter) {
            java.util.regex.Matcher matcher = sentencePattern.matcher(text);
            List<String> delimiters = new ArrayList<>();
            while (matcher.find()) delimiters.add(matcher.group());

            for (int i = 0; i < rawSentences.length; i++) {
                String s = rawSentences[i].trim();
                if (!s.isEmpty()) {
                    if (i < delimiters.size()) s += delimiters.get(i);
                    sentences.add(s);
                }
            }
        } else {
            for (String s : rawSentences) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private List<float[]> computeEmbeddings(List<String> sentences, SemanticChunkConfig config) {
        int batchSize = config.getEmbeddingBatchSize();
        List<float[]> embeddings = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sentences.size());
            try {
                embeddings.addAll(embeddingService.embed(sentences.subList(i, end)));
            } catch (Exception e) {
                log.error("Embedding 计算失败，批次起始: {}", i, e);
                for (int j = 0; j < end - i; j++) embeddings.add(new float[1024]);
            }
        }
        return embeddings;
    }

    private List<Double> computeSimilarities(List<float[]> embeddings) {
        List<Double> sims = new ArrayList<>();
        for (int i = 0; i < embeddings.size() - 1; i++) {
            sims.add(cosineSimilarity(embeddings.get(i), embeddings.get(i + 1)));
        }
        return sims;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Integer> detectBreakpoints(List<Double> similarities, SemanticChunkConfig config) {
        switch (config.getBreakpointMethod()) {
            case PERCENTILE: return detectByPercentile(similarities, config.getPercentileThreshold());
            case THRESHOLD:  return detectByThreshold(similarities, config.getSimilarityThreshold());
            case GRADIENT:   return detectByGradient(similarities);
            default:         return detectByPercentile(similarities, config.getPercentileThreshold());
        }
    }

    private List<Integer> detectByPercentile(List<Double> similarities, double percentileThreshold) {
        List<Double> sorted = new ArrayList<>(similarities);
        Collections.sort(sorted);
        int idx = Math.min((int) (percentileThreshold * sorted.size()), sorted.size() - 1);
        double threshold = sorted.get(idx);
        List<Integer> breakpoints = new ArrayList<>();
        for (int i = 0; i < similarities.size(); i++) {
            if (similarities.get(i) <= threshold) breakpoints.add(i + 1);
        }
        return breakpoints;
    }

    private List<Integer> detectByThreshold(List<Double> similarities, double threshold) {
        List<Integer> breakpoints = new ArrayList<>();
        for (int i = 0; i < similarities.size(); i++) {
            if (similarities.get(i) < threshold) breakpoints.add(i + 1);
        }
        return breakpoints;
    }

    private List<Integer> detectByGradient(List<Double> similarities) {
        List<Integer> breakpoints = new ArrayList<>();
        if (similarities.size() < 3) return breakpoints;

        List<Double> gradients = new ArrayList<>();
        for (int i = 1; i < similarities.size(); i++) {
            gradients.add(similarities.get(i) - similarities.get(i - 1));
        }
        double mean = gradients.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = Math.sqrt(gradients.stream().mapToDouble(g -> Math.pow(g - mean, 2)).average().orElse(0.0));
        double threshold = mean - stdDev;

        for (int i = 0; i < gradients.size(); i++) {
            if (gradients.get(i) < threshold) breakpoints.add(i + 2);
        }
        return breakpoints;
    }

    private List<TextChunk> createChunks(List<String> sentences, List<Integer> breakpoints,
                                          SemanticChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        int minSize = config.getMinChunkSize(), maxSize = config.getMaxChunkSize();

        List<Integer> all = new ArrayList<>();
        all.add(0);
        all.addAll(breakpoints.stream().sorted().collect(Collectors.toList()));
        all.add(sentences.size());

        List<Integer> sorted = new ArrayList<>(new TreeSet<>(all));
        int currentPos = 0;

        for (int i = 0; i < sorted.size() - 1; i++) {
            int start = sorted.get(i), end = sorted.get(i + 1);
            StringBuilder sb = new StringBuilder();
            for (int j = start; j < end; j++) {
                sb.append(sentences.get(j));
                if (j < end - 1) sb.append(" ");
            }
            String content = sb.toString().trim();

            if (content.length() < minSize && !chunks.isEmpty()) {
                TextChunk last = chunks.get(chunks.size() - 1);
                String merged = last.getContent() + " " + content;
                if (merged.length() <= maxSize) {
                    chunks.set(chunks.size() - 1, TextChunk.builder()
                            .content(merged).index(last.getIndex())
                            .startPos(last.getStartPos()).endPos(last.getEndPos() + content.length() + 1).build());
                    continue;
                }
            }
            chunks.add(TextChunk.builder()
                    .content(content).index(chunks.size())
                    .startPos(currentPos).endPos(currentPos + content.length()).build());
            currentPos += content.length();
        }
        return chunks;
    }

    private SemanticChunkConfig convertConfig(ChunkConfig config) {
        SemanticChunkConfig.SemanticChunkConfigBuilder builder = SemanticChunkConfig.builder();
        if (config.getChunkSize() > 0) builder.maxChunkSize(config.getChunkSize());
        if (config.getMinParagraphLength() > 0) builder.minChunkSize(config.getMinParagraphLength());
        return builder.build();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
