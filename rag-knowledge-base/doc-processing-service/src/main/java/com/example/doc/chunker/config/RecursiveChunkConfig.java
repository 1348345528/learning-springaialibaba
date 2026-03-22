package com.example.doc.chunker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 递归分块器配置类
 * <p>
 * 用于配置递归分块策略的各项参数，包括分块大小、重叠长度、分隔符优先级等。
 * 递归分块器会按照分隔符优先级依次尝试分割文本，直到每个分块大小满足要求。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursiveChunkConfig {

    /**
     * 默认分块大小（字符数）
     */
    public static final int DEFAULT_CHUNK_SIZE = 500;

    /**
     * 默认重叠大小（字符数）
     */
    public static final int DEFAULT_OVERLAP = 50;

    /**
     * 默认最小分块大小（字符数）
     */
    public static final int DEFAULT_MIN_CHUNK_SIZE = 50;

    /**
     * 默认分隔符优先级列表
     * 按优先级从高到低排列：三级换行 → 双换行 → 单换行 → 中文句号 → 英文句号 → 逗号 → 空格 → 空字符串
     */
    public static final List<String> DEFAULT_SEPARATORS = List.of(
            "\n\n\n",  // 三级换行（段落分隔）
            "\n\n",    // 双换行（段落分隔）
            "\n",      // 单换行
            "。",      // 中文句号
            ".",       // 英文句号
            ",",       // 逗号
            " ",       // 空格
            ""         // 空字符串（最后按字符分割）
    );

    /**
     * 目标分块大小（字符数）
     * <p>
     * 每个分块的目标最大长度。实际分块可能会因为保留完整句子或段落而略有超出。
     * </p>
     */
    @NotNull
    @Min(value = 50, message = "分块大小不能小于50字符")
    @Max(value = 10000, message = "分块大小不能超过10000字符")
    @Builder.Default
    private Integer chunkSize = DEFAULT_CHUNK_SIZE;

    /**
     * 分块之间的重叠大小（字符数）
     * <p>
     * 相邻分块之间的重叠区域大小，有助于保持上下文连贯性。
     * 重叠区域可以确保跨分块的语义不会丢失。
     * </p>
     */
    @NotNull
    @Min(value = 0, message = "重叠大小不能为负数")
    @Max(value = 500, message = "重叠大小不能超过500字符")
    @Builder.Default
    private Integer overlap = DEFAULT_OVERLAP;

    /**
     * 最小分块大小（字符数）
     * <p>
     * 分块的最小有效长度。小于此长度的分块会被合并到相邻分块中。
     * </p>
     */
    @NotNull
    @Min(value = 10, message = "最小分块大小不能小于10字符")
    @Max(value = 1000, message = "最小分块大小不能超过1000字符")
    @Builder.Default
    private Integer minChunkSize = DEFAULT_MIN_CHUNK_SIZE;

    /**
     * 是否保留分隔符
     * <p>
     * 如果为 true，分割后的文本会保留分隔符（如句号、换行符等），
     * 有助于保持文本的完整性和可读性。
     * </p>
     */
    @Builder.Default
    private Boolean keepSeparator = true;

    /**
     * 自定义分隔符列表
     * <p>
     * 如果指定，将覆盖默认分隔符优先级列表。
     * 分隔符按列表顺序从高到低排列。
     * </p>
     */
    private List<String> separators;

    /**
     * 是否去除首尾空白
     * <p>
     * 如果为 true，每个分块会自动去除首尾的空白字符。
     * </p>
     */
    @Builder.Default
    private Boolean trimWhitespace = true;

    /**
     * 获取有效分隔符列表
     *
     * @return 分隔符列表，如果未指定则返回默认列表
     */
    public List<String> getEffectiveSeparators() {
        return separators != null && !separators.isEmpty()
                ? separators
                : DEFAULT_SEPARATORS;
    }

    /**
     * 创建默认配置
     *
     * @return 默认的递归分块配置
     */
    public static RecursiveChunkConfig defaultConfig() {
        return RecursiveChunkConfig.builder().build();
    }

    /**
     * 创建指定分块大小的配置
     *
     * @param chunkSize 目标分块大小
     * @return 递归分块配置
     */
    public static RecursiveChunkConfig of(int chunkSize) {
        return RecursiveChunkConfig.builder()
                .chunkSize(chunkSize)
                .build();
    }

    /**
     * 创建指定分块大小和重叠大小的配置
     *
     * @param chunkSize 目标分块大小
     * @param overlap   重叠大小
     * @return 递归分块配置
     */
    public static RecursiveChunkConfig of(int chunkSize, int overlap) {
        return RecursiveChunkConfig.builder()
                .chunkSize(chunkSize)
                .overlap(overlap)
                .build();
    }
}
