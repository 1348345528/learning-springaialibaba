package com.example.doc.controller;

import com.example.doc.dto.ChunkPreviewRequest;
import com.example.doc.dto.ChunkPreviewResponse;
import com.example.doc.service.ChunkPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分块预览控制器
 * <p>
 * 提供分块预览相关的 REST API 接口。允许用户在不实际保存分块的情况下
 * 查看不同分块策略的效果，便于调试和优化分块参数。
 * </p>
 *
 * @author AI Engineer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/chunk/preview")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "分块预览", description = "分块预览相关接口")
public class ChunkPreviewController {

    private final ChunkPreviewService chunkPreviewService;

    /**
     * 预览分块效果
     * <p>
     * 根据指定的策略和参数预览文本分块效果，返回详细的分块信息和统计数据。
     * </p>
     *
     * @param request 预览请求
     * @return 预览响应
     */
    @PostMapping
    @Operation(
            summary = "预览分块效果",
            description = "根据指定的策略和参数预览文本分块效果，返回详细的分块信息和统计数据",
            responses = {
                    @ApiResponse(responseCode = "200", description = "预览成功",
                            content = @Content(schema = @Schema(implementation = ChunkPreviewResponse.class))),
                    @ApiResponse(responseCode = "400", description = "请求参数无效"),
                    @ApiResponse(responseCode = "500", description = "服务器内部错误")
            }
    )
    public ResponseEntity<ChunkPreviewResponse> preview(
            @Valid @RequestBody ChunkPreviewRequest request) {
        ChunkPreviewResponse response = chunkPreviewService.preview(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 快速预览递归分块
     * <p>
     * 使用默认参数快速预览递归分块效果。
     * </p>
     *
     * @param content   待分块的文本内容
     * @param chunkSize 目标分块大小（可选，默认500）
     * @param overlap   重叠大小（可选，默认50）
     * @return 预览响应
     */
    @PostMapping("/recursive")
    @Operation(
            summary = "快速预览递归分块",
            description = "使用默认参数快速预览递归分块效果"
    )
    public ResponseEntity<ChunkPreviewResponse> previewRecursive(
            @Parameter(description = "待分块的文本内容") @RequestBody String content,
            @Parameter(description = "目标分块大小") @RequestParam(required = false, defaultValue = "500") Integer chunkSize,
            @Parameter(description = "重叠大小") @RequestParam(required = false, defaultValue = "50") Integer overlap) {

        ChunkPreviewRequest request = ChunkPreviewRequest.builder()
                .content(content)
                .strategy("recursive")
                .chunkSize(chunkSize)
                .overlap(overlap)
                .build();

        return ResponseEntity.ok(chunkPreviewService.preview(request));
    }

    /**
     * 快速预览语义分块
     * <p>
     * 使用默认参数快速预览语义分块效果。
     * </p>
     *
     * @param content            待分块的文本内容
     * @param similarityThreshold 相似度阈值（可选，默认0.45）
     * @param breakpointMethod   断点检测方法（可选，默认PERCENTILE）
     * @return 预览响应
     */
    @PostMapping("/semantic")
    @Operation(
            summary = "快速预览语义分块",
            description = "使用默认参数快速预览语义分块效果"
    )
    public ResponseEntity<ChunkPreviewResponse> previewSemantic(
            @Parameter(description = "待分块的文本内容") @RequestBody String content,
            @Parameter(description = "相似度阈值") @RequestParam(required = false, defaultValue = "0.45") Double similarityThreshold,
            @Parameter(description = "断点检测方法") @RequestParam(required = false, defaultValue = "PERCENTILE") String breakpointMethod) {

        ChunkPreviewRequest request = ChunkPreviewRequest.builder()
                .content(content)
                .strategy("true_semantic")
                .similarityThreshold(similarityThreshold)
                .breakpointMethod(breakpointMethod)
                .build();

        return ResponseEntity.ok(chunkPreviewService.preview(request));
    }

    /**
     * 快速预览分层分块
     * <p>
     * 使用默认参数快速预览分层分块效果。
     * </p>
     *
     * @param content         待分块的文本内容
     * @param parentChunkSize 父块大小（可选，默认2000）
     * @param childChunkSize  子块大小（可选，默认200）
     * @param childOverlap    子块重叠（可选，默认20）
     * @return 预览响应
     */
    @PostMapping("/hierarchical")
    @Operation(
            summary = "快速预览分层分块",
            description = "使用默认参数快速预览分层分块效果"
    )
    public ResponseEntity<ChunkPreviewResponse> previewHierarchical(
            @Parameter(description = "待分块的文本内容") @RequestBody String content,
            @Parameter(description = "父块大小") @RequestParam(required = false, defaultValue = "2000") Integer parentChunkSize,
            @Parameter(description = "子块大小") @RequestParam(required = false, defaultValue = "200") Integer childChunkSize,
            @Parameter(description = "子块重叠") @RequestParam(required = false, defaultValue = "20") Integer childOverlap) {

        ChunkPreviewRequest request = ChunkPreviewRequest.builder()
                .content(content)
                .strategy("hierarchical")
                .parentChunkSize(parentChunkSize)
                .childChunkSize(childChunkSize)
                .childOverlap(childOverlap)
                .build();

        return ResponseEntity.ok(chunkPreviewService.preview(request));
    }

    /**
     * 获取支持的分块策略列表
     *
     * @return 策略列表
     */
    @GetMapping("/strategies")
    @Operation(
            summary = "获取支持的分块策略",
            description = "返回所有支持的分块策略及其说明"
    )
    public ResponseEntity<List<Map<String, Object>>> getStrategies() {
        List<Map<String, Object>> strategies = List.of(
                Map.of(
                        "name", "recursive",
                        "description", "递归分块器：使用多级分隔符递归切分文本",
                        "defaultParams", Map.of(
                                "chunkSize", 500,
                                "overlap", 50,
                                "minChunkSize", 50,
                                "keepSeparator", true
                        )
                ),
                Map.of(
                        "name", "true_semantic",
                        "description", "语义分块器：基于句子Embedding相似度进行边界检测",
                        "defaultParams", Map.of(
                                "similarityThreshold", 0.45,
                                "percentileThreshold", 0.8,
                                "breakpointMethod", "PERCENTILE",
                                "useDynamicThreshold", true
                        )
                ),
                Map.of(
                        "name", "hierarchical",
                        "description", "分层分块器：创建父子结构的两层分块",
                        "defaultParams", Map.of(
                                "parentChunkSize", 2000,
                                "childChunkSize", 200,
                                "childOverlap", 20,
                                "childSplitStrategy", "RECURSIVE"
                        )
                )
        );
        return ResponseEntity.ok(strategies);
    }

    /**
     * 比较不同分块策略的效果
     * <p>
     * 对同一段文本使用不同策略进行分块，返回对比结果。
     * </p>
     *
     * @param content   待分块的文本内容
     * @param chunkSize 目标分块大小
     * @return 各策略的对比结果
     */
    @PostMapping("/compare")
    @Operation(
            summary = "比较不同分块策略",
            description = "对同一段文本使用不同策略进行分块，返回对比结果"
    )
    public ResponseEntity<Map<String, ChunkPreviewResponse>> compareStrategies(
            @Parameter(description = "待分块的文本内容") @RequestBody String content,
            @Parameter(description = "目标分块大小") @RequestParam(required = false, defaultValue = "500") Integer chunkSize) {

        Map<String, ChunkPreviewResponse> results = new java.util.LinkedHashMap<>();

        // 递归分块
        ChunkPreviewRequest recursiveRequest = ChunkPreviewRequest.builder()
                .content(content)
                .strategy("recursive")
                .chunkSize(chunkSize)
                .build();
        results.put("recursive", chunkPreviewService.preview(recursiveRequest));

        // 语义分块
        ChunkPreviewRequest semanticRequest = ChunkPreviewRequest.builder()
                .content(content)
                .strategy("true_semantic")
                .maxChunkSize(chunkSize)
                .build();
        results.put("true_semantic", chunkPreviewService.preview(semanticRequest));

        // 分层分块
        ChunkPreviewRequest hierarchicalRequest = ChunkPreviewRequest.builder()
                .content(content)
                .strategy("hierarchical")
                .parentChunkSize(chunkSize * 4)
                .childChunkSize(chunkSize / 2)
                .build();
        results.put("hierarchical", chunkPreviewService.preview(hierarchicalRequest));

        return ResponseEntity.ok(results);
    }
}
