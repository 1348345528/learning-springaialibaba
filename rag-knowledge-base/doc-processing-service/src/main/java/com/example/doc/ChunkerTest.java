package com.example.doc;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.TextChunk;
import com.example.doc.chunker.impl.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 分块器测试类 - 按三种策略分块 Word 文档，结果输出到独立文件（每行一个分块）
 */
public class ChunkerTest {

    private static final String FILE_PATH = "C:/Users/xiongzhuang/Desktop/rag/中国农再收付费系统需求规格说明书V2.2【签字版本】.docx";
    private static final String OUTPUT_DIR = "target/";

    public static void main(String[] args) throws IOException {
        // 1. 读取 Word 文档
        String text = readWordDocument(FILE_PATH);
        System.out.println("文档读取完成，总长度: " + text.length() + " 字符\n");

        // 2. 配置
        ChunkConfig config = new ChunkConfig();
        config.setChunkSize(500);
        config.setOverlap(50);
        config.setMinParagraphLength(50);

        // 3. 递归分块
        List<TextChunk> recursiveChunks = new RecursiveChunker().chunk(text, config);
        writeChunksToFile(OUTPUT_DIR + "recursive.txt", recursiveChunks);
        System.out.println("recursive.txt  - " + recursiveChunks.size() + " 个分块");

        // 4. 语义边界分块
        List<TextChunk> boundaryChunks = new SemanticBoundaryChunker().chunk(text, config);
        writeChunksToFile(OUTPUT_DIR + "semantic_boundary.txt", boundaryChunks);
        System.out.println("semantic_boundary.txt - " + boundaryChunks.size() + " 个分块");

        // 5. 分层分块
        RecursiveChunker rc = new RecursiveChunker();
        List<TextChunk> hierChunks = new HierarchicalChunker(rc).chunk(text, config);
        writeChunksToFile(OUTPUT_DIR + "hierarchical.txt", hierChunks);
        System.out.println("hierarchical.txt - " + hierChunks.size() + " 个分块");

        System.out.println("\n完成，文件输出到 doc-processing-service/target/ 目录");
    }

    private static String readWordDocument(String filePath) throws IOException {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.trim().isEmpty()) {
                    String styleName = paragraph.getStyle();
                    if (styleName != null && styleName.startsWith("Heading")) {
                        int level = Math.min(getHeadingLevel(styleName), 3);
                        text.append("#".repeat(level))
                                .append(" ")
                                .append(paraText)
                                .append("\n\n");
                    } else {
                        text.append(paraText).append("\n\n");
                    }
                }
            }
        }
        return text.toString();
    }

    private static int getHeadingLevel(String styleName) {
        try {
            if (styleName.matches("Heading\\d+")) {
                return Integer.parseInt(styleName.replace("Heading", ""));
            }
        } catch (NumberFormatException ignored) {
        }
        return 1;
    }

    /**
     * 每个分块压缩为一行写入文件
     */
    private static void writeChunksToFile(String filePath, List<TextChunk> chunks) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            for (TextChunk chunk : chunks) {
                // 将分块内容压缩为一行：换行替换为空格，去掉多余空白
                String line = chunk.getContent()
                        .replace("\r\n", " ")
                        .replace("\n", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                writer.write(line);
                writer.newLine();
            }
        }
    }
}
