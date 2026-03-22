package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import com.example.doc.chunker.config.SemanticChunkConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 真正的语义分块器
 * <p>
 * 基于句子 Embedding 相似度进行边界检测的智能分块器。通过计算相邻句子之间的
 * 语义相似度来识别主题边界，从而实现更加智能和语义连贯的分块效果。
 * </p>
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>将文本分割成句子</li>
 *   <li>为每个句子计算 Embedding 向量</li>
 *   <li>计算相邻句子之间的语义相似度（余弦相似度）</li>
 *   <li>根据断点检测方法识别语义边界</li>
 *   <li>在边界位置创建分块</li>
 * </ol>
 *
 * <h3>断点检测方法</h3>
 * <ul>
 *   <li><b>PERCENTILE</b>：选择相似度最低的 X% 位置作为断点</li>
 *   <li><b>THRESHOLD</b>：相似度低于固定阈值时创建断点</li>
 *   <li><b>GRADIENT</b>：检测相似度变化的梯度来识别边界</li>
 * </ul>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrueSemanticChunker implements ChunkStrategy {

    /**
     * 分块器名称
     */
    private static final String NAME = "true_semantic";

    /**
     * 默认语义配置
     */
    private static final SemanticChunkConfig DEFAULT_CONFIG = SemanticChunkConfig.defaultConfig();

    @Value("${app.rag-service-url:http://localhost:8081}")
    private String ragServiceUrl;

    /**
     * 获取 RAG 服务的 WebClient
     */
    private WebClient getRagServiceClient() {
        return WebClient.create(ragServiceUrl);
    }

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        // 将通用配置转换为语义分块配置
        SemanticChunkConfig semanticConfig = convertConfig(config);
        return chunk(text, semanticConfig);
    }

    /**
     * 使用语义配置进行分块
     *
     * @param text   待分块的文本
     * @param config 语义分块配置
     * @return 分块列表
     */
    public List<TextChunk> chunk(String text, SemanticChunkConfig config) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用配置或默认值
        SemanticChunkConfig effectiveConfig = config != null ? config : DEFAULT_CONFIG;

        log.debug("开始语义分块，断点检测方法: {}, 相似度阈值: {}",
                effectiveConfig.getBreakpointMethod(),
                effectiveConfig.getSimilarityThreshold());

        // Step 1: 分割句子
        List<String> sentences = splitIntoSentences(text, effectiveConfig);
        log.debug("分割出 {} 个句子", sentences.size());

        if (sentences.size() <= 1) {
            // 只有一个句子，直接返回
            return List.of(TextChunk.builder()
                    .content(text.trim())
                    .index(0)
                    .startPos(0)
                    .endPos(text.length())
                    .build());
        }

        // Step 2: 计算 Embeddings
        List<float[]> embeddings = computeEmbeddings(sentences, effectiveConfig);
        log.debug("计算了 {} 个 Embedding 向量", embeddings.size());

        // Step 3: 计算相邻句子之间的相似度
        List<Double> similarities = computeSimilarities(embeddings);
        log.debug("计算了 {} 个相似度值", similarities.size());

        // Step 4: 检测断点
        List<Integer> breakpoints = detectBreakpoints(similarities, effectiveConfig);
        log.debug("检测到 {} 个断点", breakpoints.size());

        // Step 5: 根据断点创建分块
        List<TextChunk> chunks = createChunks(sentences, breakpoints, effectiveConfig);

        log.info("语义分块完成，生成分块数量: {}", chunks.size());
        return chunks;
    }

    /**
     * 将文本分割成句子
     *
     * @param text   待分割的文本
     * @param config 语义分块配置
     * @return 句子列表
     */
    private List<String> splitIntoSentences(String text, SemanticChunkConfig config) {
        String pattern = config.getSentenceSplitPattern();
        boolean keepDelimiter = Boolean.TRUE.equals(config.getKeepSentenceDelimiter());

        // 使用正则表达式分割句子
        Pattern sentencePattern = Pattern.compile(pattern);
        String[] rawSentences = sentencePattern.split(text);

        List<String> sentences = new ArrayList<>();

        // 重新组合句子和分隔符
        if (keepDelimiter) {
            // 使用正则查找所有分隔符
            java.util.regex.Matcher matcher = sentencePattern.matcher(text);
            List<String> delimiters = new ArrayList<>();
            while (matcher.find()) {
                delimiters.add(matcher.group());
            }

            for (int i = 0; i < rawSentences.length; i++) {
                String sentence = rawSentences[i].trim();
                if (!sentence.isEmpty()) {
                    if (i < delimiters.size()) {
                        sentence += delimiters.get(i);
                    }
                    sentences.add(sentence);
                }
            }
        } else {
            for (String sentence : rawSentences) {
                sentence = sentence.trim();
                if (!sentence.isEmpty()) {
                    sentences.add(sentence);
                }
            }
        }

        return sentences;
    }

    /**
     * 批量计算句子的 Embedding 向量
     *
     * @param sentences 句子列表
     * @param config    配置
     * @return Embedding 向量列表
     */
    private List<float[]> computeEmbeddings(List<String> sentences, SemanticChunkConfig config) {
        List<float[]> embeddings = new ArrayList<>();
        int batchSize = config.getEmbeddingBatchSize();

        // 分批计算 Embedding
        for (int i = 0; i < sentences.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sentences.size());
            List<String> batch = sentences.subList(i, end);

            try {
                // 调用 Embedding 服务
                List<float[]> batchEmbeddings = callEmbeddingService(batch);
                embeddings.addAll(batchEmbeddings);
            } catch (Exception e) {
                log.error("调用 Embedding 服务失败，批次起始索引: {}", i, e);
                // 使用零向量作为 fallback
                for (int j = 0; j < batch.size(); j++) {
                    embeddings.add(new float[1536]); // 假设使用 1536 维向量
                }
            }
        }

        return embeddings;
    }

    /**
     * 调用 Embedding 服务计算向量
     *
     * @param texts 文本列表
     * @return Embedding 向量列表
     */
    @SuppressWarnings("unchecked")
    private List<float[]> callEmbeddingService(List<String> texts) {
        try {
            // 调用 RAG 服务的 Embedding 接口
            Map<String, Object> response = getRagServiceClient().post()
                    .uri("/api/embedding/batch")
                    .bodyValue(Map.of("texts", texts))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("embeddings")) {
                List<List<Double>> embeddingList = (List<List<Double>>) response.get("embeddings");
                return embeddingList.stream()
                        .map(this::toFloatArray)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("调用 Embedding 服务失败，使用本地模拟: {}", e.getMessage());
        }

        // 如果远程服务不可用，使用简单的本地模拟
        return texts.stream()
                .map(this::simulateEmbedding)
                .collect(Collectors.toList());
    }

    /**
     * 将 Double 列表转换为 float 数组
     */
    private float[] toFloatArray(List<Double> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).floatValue();
        }
        return arr;
    }

    /**
     * 简单的本地 Embedding 模拟（当远程服务不可用时）
     * <p>
     * 注意：这只是 fallback 方案，实际使用应调用真正的 Embedding 服务
     * </p>
     */
    private float[] simulateEmbedding(String text) {
        // 使用简单的字符哈希生成伪向量（仅用于 fallback）
        int dim = 1536;
        float[] embedding = new float[dim];
        int hash = text.hashCode();

        for (int i = 0; i < dim; i++) {
            embedding[i] = (float) Math.sin(hash + i) * 0.1f;
        }

        // L2 归一化
        float norm = 0f;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < dim; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }

    /**
     * 计算相邻句子之间的余弦相似度
     *
     * @param embeddings Embedding 向量列表
     * @return 相似度列表（长度为 n-1）
     */
    private List<Double> computeSimilarities(List<float[]> embeddings) {
        List<Double> similarities = new ArrayList<>();

        for (int i = 0; i < embeddings.size() - 1; i++) {
            double similarity = cosineSimilarity(embeddings.get(i), embeddings.get(i + 1));
            similarities.add(similarity);
        }

        return similarities;
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param a 向量 A
     * @param b 向量 B
     * @return 余弦相似度（-1 到 1）
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 检测断点位置
     *
     * @param similarities 相似度列表
     * @param config       配置
     * @return 断点位置列表（句子索引）
     */
    private List<Integer> detectBreakpoints(List<Double> similarities, SemanticChunkConfig config) {
        List<Integer> breakpoints = new ArrayList<>();

        switch (config.getBreakpointMethod()) {
            case PERCENTILE:
                breakpoints = detectBreakpointsByPercentile(similarities, config.getPercentileThreshold());
                break;
            case THRESHOLD:
                breakpoints = detectBreakpointsByThreshold(similarities, config.getSimilarityThreshold());
                break;
            case GRADIENT:
                breakpoints = detectBreakpointsByGradient(similarities);
                break;
            default:
                breakpoints = detectBreakpointsByPercentile(similarities, config.getPercentileThreshold());
        }

        return breakpoints;
    }

    /**
     * 使用百分位数法检测断点
     * <p>
     * 选择相似度低于某个百分位的句子边界作为断点。
     * </p>
     *
     * @param similarities      相似度列表
     * @param percentileThreshold 百分位数阈值（0.8 表示选择相似度最低的 20%）
     * @return 断点位置列表
     */
    private List<Integer> detectBreakpointsByPercentile(List<Double> similarities, double percentileThreshold) {
        List<Integer> breakpoints = new ArrayList<>();

        // 复制并排序相似度值
        List<Double> sortedSimilarities = new ArrayList<>(similarities);
        Collections.sort(sortedSimilarities);

        // 计算百分位阈值
        int index = (int) (percentileThreshold * sortedSimilarities.size());
        if (index >= sortedSimilarities.size()) {
            index = sortedSimilarities.size() - 1;
        }
        double threshold = sortedSimilarities.get(index);

        // 找出所有低于阈值的相似度对应的断点
        for (int i = 0; i < similarities.size(); i++) {
            if (similarities.get(i) <= threshold) {
                breakpoints.add(i + 1); // 断点位置是句子索引
            }
        }

        return breakpoints;
    }

    /**
     * 使用阈值法检测断点
     *
     * @param similarities 相似度列表
     * @param threshold    相似度阈值
     * @return 断点位置列表
     */
    private List<Integer> detectBreakpointsByThreshold(List<Double> similarities, double threshold) {
        List<Integer> breakpoints = new ArrayList<>();

        for (int i = 0; i < similarities.size(); i++) {
            if (similarities.get(i) < threshold) {
                breakpoints.add(i + 1);
            }
        }

        return breakpoints;
    }

    /**
     * 使用梯度法检测断点
     * <p>
     * 检测相似度变化的梯度来识别边界。
     * </p>
     *
     * @param similarities 相似度列表
     * @return 断点位置列表
     */
    private List<Integer> detectBreakpointsByGradient(List<Double> similarities) {
        List<Integer> breakpoints = new ArrayList<>();

        if (similarities.size() < 3) {
            return breakpoints;
        }

        // 计算梯度
        List<Double> gradients = new ArrayList<>();
        for (int i = 1; i < similarities.size(); i++) {
            gradients.add(similarities.get(i) - similarities.get(i - 1));
        }

        // 计算梯度的标准差
        double mean = gradients.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = gradients.stream()
                .mapToDouble(g -> Math.pow(g - mean, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // 找出梯度显著下降的点（低于均值减去一个标准差）
        double threshold = mean - stdDev;
        for (int i = 0; i < gradients.size(); i++) {
            if (gradients.get(i) < threshold) {
                breakpoints.add(i + 2); // +2 是因为梯度从第二个相似度开始计算
            }
        }

        return breakpoints;
    }

    /**
     * 根据断点创建分块
     *
     * @param sentences  句子列表
     * @param breakpoints 断点位置列表
     * @param config     配置
     * @return 分块列表
     */
    private List<TextChunk> createChunks(List<String> sentences, List<Integer> breakpoints,
                                          SemanticChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        int minChunkSize = config.getMinChunkSize();
        int maxChunkSize = config.getMaxChunkSize();

        // 添加首尾断点
        List<Integer> allBreakpoints = new ArrayList<>();
        allBreakpoints.add(0);
        allBreakpoints.addAll(breakpoints.stream().sorted().collect(Collectors.toList()));
        allBreakpoints.add(sentences.size());

        // 去重并排序
        Set<Integer> uniqueBreakpoints = new TreeSet<>(allBreakpoints);
        List<Integer> sortedBreakpoints = new ArrayList<>(uniqueBreakpoints);

        int currentPos = 0;

        for (int i = 0; i < sortedBreakpoints.size() - 1; i++) {
            int start = sortedBreakpoints.get(i);
            int end = sortedBreakpoints.get(i + 1);

            // 合并句子
            StringBuilder content = new StringBuilder();
            for (int j = start; j < end; j++) {
                content.append(sentences.get(j));
                if (j < end - 1) {
                    content.append(" ");
                }
            }

            String chunkContent = content.toString().trim();

            // 检查分块大小
            if (chunkContent.length() < minChunkSize && chunks.size() > 0) {
                // 过小的分块合并到前一个分块
                TextChunk lastChunk = chunks.get(chunks.size() - 1);
                String mergedContent = lastChunk.getContent() + " " + chunkContent;
                if (mergedContent.length() <= maxChunkSize) {
                    chunks.set(chunks.size() - 1, TextChunk.builder()
                            .content(mergedContent)
                            .index(lastChunk.getIndex())
                            .startPos(lastChunk.getStartPos())
                            .endPos(lastChunk.getEndPos() + chunkContent.length() + 1)
                            .build());
                    continue;
                }
            }

            // 处理过大的分块
            if (chunkContent.length() > maxChunkSize) {
                // 使用递归分块器进一步分割
                List<TextChunk> subChunks = splitLargeChunk(chunkContent, maxChunkSize, minChunkSize, currentPos);
                for (TextChunk subChunk : subChunks) {
                    chunks.add(TextChunk.builder()
                            .content(subChunk.getContent())
                            .index(chunks.size())
                            .startPos(subChunk.getStartPos())
                            .endPos(subChunk.getEndPos())
                            .build());
                }
                currentPos += chunkContent.length();
            } else {
                // 创建正常分块
                TextChunk chunk = TextChunk.builder()
                        .content(chunkContent)
                        .index(chunks.size())
                        .startPos(currentPos)
                        .endPos(currentPos + chunkContent.length())
                        .build();
                chunks.add(chunk);
                currentPos += chunkContent.length();
            }
        }

        return chunks;
    }

    /**
     * 分割过大的分块
     *
     * @param content      分块内容
     * @param maxChunkSize 最大分块大小
     * @param minChunkSize 最小分块大小
     * @param startPos     起始位置
     * @return 子分块列表
     */
    private List<TextChunk> splitLargeChunk(String content, int maxChunkSize, int minChunkSize, int startPos) {
        List<TextChunk> subChunks = new ArrayList<>();
        int pos = 0;

        while (pos < content.length()) {
            int end = Math.min(pos + maxChunkSize, content.length());

            // 尝试在句子边界处分割
            if (end < content.length()) {
                int lastSentenceEnd = findLastSentenceEnd(content, pos, end);
                if (lastSentenceEnd > pos + minChunkSize) {
                    end = lastSentenceEnd;
                }
            }

            String subContent = content.substring(pos, end).trim();
            if (!subContent.isEmpty()) {
                subChunks.add(TextChunk.builder()
                        .content(subContent)
                        .index(subChunks.size())
                        .startPos(startPos + pos)
                        .endPos(startPos + end)
                        .build());
            }
            pos = end;
        }

        return subChunks;
    }

    /**
     * 查找指定范围内的最后一个句子结束位置
     *
     * @param content  内容
     * @param start    开始位置
     * @param end      结束位置
     * @return 句子结束位置
     */
    private int findLastSentenceEnd(String content, int start, int end) {
        String subContent = content.substring(start, end);
        String[] delimiters = {"。", ".", "！", "!", "？", "?"};

        int lastEnd = -1;
        for (String delimiter : delimiters) {
            int idx = subContent.lastIndexOf(delimiter);
            if (idx > lastEnd) {
                lastEnd = idx + delimiter.length();
            }
        }

        return lastEnd > 0 ? start + lastEnd : end;
    }

    /**
     * 将通用 ChunkConfig 转换为 SemanticChunkConfig
     *
     * @param config 通用配置
     * @return 语义分块配置
     */
    private SemanticChunkConfig convertConfig(ChunkConfig config) {
        SemanticChunkConfig.SemanticChunkConfigBuilder builder = SemanticChunkConfig.builder();

        if (config.getChunkSize() > 0) {
            builder.maxChunkSize(config.getChunkSize());
        }
        if (config.getMinParagraphLength() > 0) {
            builder.minChunkSize(config.getMinParagraphLength());
        }

        return builder.build();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
