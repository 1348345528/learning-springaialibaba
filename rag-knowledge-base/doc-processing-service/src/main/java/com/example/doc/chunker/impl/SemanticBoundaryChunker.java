package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义边界分块器
 * <p>
 * 只在完整的句子/段落边界处分割，绝不切断句子。
 * 优先按段落分割，其次按句子分割，最后按换行分割。
 * </p>
 *
 * <h3>分割优先级</h3>
 * <ol>
 *   <li>段落边界：双换行 (\n\n, \r\n\r\n)</li>
 *   <li>句子边界：。！？；.!?;</li>
 *   <li>换行边界：\n, \r\n</li>
 *   <li>逗号边界：，,</li>
 * </ol>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Component
@Slf4j
public class SemanticBoundaryChunker implements ChunkStrategy {

    private static final String NAME = "semantic_boundary";

    /**
     * 句子结束符正则
     * 匹配：。！？；.!?; 以及这些字符后面的引号
     */
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(
            "[。！？；.!?;][\"'”’『』）]?\\s*"
    );

    /**
     * 换行符正则（兼容 Windows/Unix）
     */
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\r?\\n");

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        int chunkSize = config.getChunkSize();
        int minChunkSize = config.getMinParagraphLength();

        // 标准化换行符
        text = text.replace("\r\n", "\n");

        // Step 1: 按段落分割
        List<String> paragraphs = splitByParagraph(text);

        // Step 2: 按 chunkSize 合并段落，确保不切断句子
        List<TextChunk> chunks = mergeByChunkSize(paragraphs, chunkSize, minChunkSize);

        log.info("语义边界分块完成，生成分块数量: {}", chunks.size());
        return chunks;
    }

    /**
     * 按段落分割文本
     */
    private List<String> splitByParagraph(String text) {
        List<String> paragraphs = new ArrayList<>();

        // 按双换行分割段落
        String[] parts = text.split("\n\n+");

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                // 进一步按句子分割，保留句子完整性
                List<String> sentences = splitBySentence(part);
                paragraphs.addAll(sentences);
            }
        }

        return paragraphs;
    }

    /**
     * 按句子分割文本
     * <p>
     * 在句子边界处分割，保留句子完整性。
     * </p>
     */
    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();

        // 如果文本很短，直接返回
        if (text.length() <= 100) {
            sentences.add(text);
            return sentences;
        }

        // 使用正则查找句子边界
        Matcher matcher = SENTENCE_END_PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            int end = matcher.end();
            String sentence = text.substring(lastEnd, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = end;
        }

        // 处理剩余部分
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                // 如果剩余部分很长，按换行分割
                if (remaining.length() > 200) {
                    List<String> lines = splitByLine(remaining);
                    sentences.addAll(lines);
                } else {
                    sentences.add(remaining);
                }
            }
        }

        return sentences;
    }

    /**
     * 按换行分割文本
     */
    private List<String> splitByLine(String text) {
        List<String> lines = new ArrayList<>();

        String[] parts = LINE_BREAK_PATTERN.split(text);
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                lines.add(part);
            }
        }

        return lines;
    }

    /**
     * 按 chunkSize 合并段落，确保不切断句子
     */
    private List<TextChunk> mergeByChunkSize(List<String> paragraphs, int chunkSize, int minChunkSize) {
        List<TextChunk> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentPos = 0;
        int startPos = 0;

        for (String para : paragraphs) {
            // 如果当前块 + 新段落超过 chunkSize，保存当前块
            if (currentChunk.length() > 0 && currentChunk.length() + para.length() + 2 > chunkSize) {
                // 保存当前块
                String content = currentChunk.toString().trim();
                if (content.length() >= minChunkSize) {
                    chunks.add(TextChunk.builder()
                            .content(content)
                            .index(chunks.size())
                            .startPos(startPos)
                            .endPos(currentPos)
                            .build());
                }
                currentChunk = new StringBuilder();
                startPos = currentPos;
            }

            // 添加段落到当前块
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
                currentPos += 2;
            }
            currentChunk.append(para);
            currentPos += para.length();
        }

        // 处理最后一个块
        if (currentChunk.length() > 0) {
            String content = currentChunk.toString().trim();
            if (content.length() >= minChunkSize) {
                chunks.add(TextChunk.builder()
                        .content(content)
                        .index(chunks.size())
                        .startPos(startPos)
                        .endPos(currentPos)
                        .build());
            }
        }

        return chunks;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
