package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionInfo {
    private String name;
    private String description;
    private long rowCount;
    private boolean loaded;
    private List<FieldInfo> fields;
    private List<IndexInfo> indexes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldInfo {
        private String name;
        private String dataType;
        private boolean primaryKey;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndexInfo {
        private String fieldName;
        private String indexType;
        private String metricType;
    }
}
