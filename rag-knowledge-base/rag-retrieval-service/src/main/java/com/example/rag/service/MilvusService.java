package com.example.rag.service;

import com.example.rag.dto.CollectionInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.*;
import io.milvus.v2.service.collection.response.DescribeCollectionResp;
import io.milvus.v2.service.collection.response.GetCollectionStatsResp;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.*;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.ranker.RRFRanker;
import io.milvus.v2.service.vector.request.ranker.WeightedRanker;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilvusService {

    private final MilvusClientV2 client;

    @Value("${milvus.collection}")
    private String collectionName;

    @Value("${milvus.dimension}")
    private int dimension;

    private volatile boolean initialized = false;

    @PostConstruct
    public void initCollection() {
        initWithRetry(10, 3000);
    }

    private void initWithRetry(int maxRetries, long initialDelayMs) {
        int attempt = 0;
        long delayMs = initialDelayMs;

        while (attempt < maxRetries) {
            attempt++;
            try {
                log.info("Connecting to Milvus (attempt {}/{})", attempt, maxRetries);
                if (collectionExists()) {
                    log.info("Collection {} already exists, loading", collectionName);
                    loadCollectionIfNeeded();
                } else {
                    createCollectionWithSchema();
                }
                initialized = true;
                return;
            } catch (Exception e) {
                log.warn("Milvus init failed (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(delayMs);
                        delayMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Failed to initialize Milvus after {} attempts", maxRetries);
    }

    // ==================== Collection Management ====================

    private boolean collectionExists() {
        try {
            ListCollectionsResp resp = client.listCollections();
            return resp.getCollectionNames().contains(collectionName);
        } catch (Exception e) {
            return false;
        }
    }

    public void createCollectionWithSchema() {
        log.info("Creating collection: {}", collectionName);

        if (collectionExists()) {
            client.dropCollection(DropCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
        }

        // Build schema with all fields
        CreateCollectionReq.CollectionSchema schema = client.createSchema();
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(DataType.VarChar)
                .maxLength(36)
                .isPrimaryKey(Boolean.TRUE)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("content")
                .dataType(DataType.VarChar)
                .maxLength(65535)
                .enableAnalyzer(Boolean.TRUE)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("embedding")
                .dataType(DataType.FloatVector)
                .dimension(dimension)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("sparse_vec")
                .dataType(DataType.SparseFloatVector)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("document_name")
                .dataType(DataType.VarChar)
                .maxLength(255)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("doc_id")
                .dataType(DataType.VarChar)
                .maxLength(36)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("chunk_index")
                .dataType(DataType.Int32)
                .build());
        schema.addField(AddFieldReq.builder()
                .fieldName("created_at")
                .dataType(DataType.Int64)
                .build());

        // Add BM25 function to schema
        CreateCollectionReq.Function bm25Function = CreateCollectionReq.Function.builder()
                .functionType(FunctionType.BM25)
                .name("bm25_func")
                .inputFieldNames(Collections.singletonList("content"))
                .outputFieldNames(Collections.singletonList("sparse_vec"))
                .build();
        schema.addFunction(bm25Function);

        CreateCollectionReq createReq = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .build();

        client.createCollection(createReq);
        log.info("Collection {} created", collectionName);

        // Create indexes
        createDenseIndex();
        createSparseIndex();
        loadCollection();
    }

    public void createDenseIndex() {
        log.info("Creating HNSW index on embedding");
        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("M", 32);
        extraParams.put("efConstruction", 128);

        IndexParam param = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.HNSW)
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(extraParams)
                .build();

        client.createIndex(CreateIndexReq.builder()
                .collectionName(collectionName)
                .indexParams(Collections.singletonList(param))
                .build());
        log.info("HNSW index created on embedding");
    }

    public void createSparseIndex() {
        log.info("Creating sparse inverted index on sparse_vec");
        IndexParam param = IndexParam.builder()
                .fieldName("sparse_vec")
                .indexType(IndexParam.IndexType.SPARSE_INVERTED_INDEX)
                .metricType(IndexParam.MetricType.BM25)
                .build();

        client.createIndex(CreateIndexReq.builder()
                .collectionName(collectionName)
                .indexParams(Collections.singletonList(param))
                .build());
        log.info("Sparse inverted index created on sparse_vec");
    }

    public void loadCollection() {
        log.info("Loading collection: {}", collectionName);
        client.loadCollection(LoadCollectionReq.builder()
                .collectionName(collectionName)
                .build());
        log.info("Collection {} loaded into memory", collectionName);
    }

    private void loadCollectionIfNeeded() {
        try {
            DescribeCollectionResp desc = client.describeCollection(DescribeCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
            log.info("Collection {} exists (schema loaded), ensuring collection is loaded", desc.getCollectionName());
            loadCollection();
        } catch (Exception e) {
            log.warn("Failed to load collection: {}", e.getMessage());
        }
    }

    public CollectionInfo getCollectionInfo() {
        DescribeCollectionResp desc = client.describeCollection(DescribeCollectionReq.builder()
                .collectionName(collectionName)
                .build());

        GetCollectionStatsResp stats = client.getCollectionStats(GetCollectionStatsReq.builder()
                .collectionName(collectionName)
                .build());

        // Fields from CollectionSchema
        CreateCollectionReq.CollectionSchema schema = desc.getCollectionSchema();
        List<CollectionInfo.FieldInfo> fields = new ArrayList<>();
        if (schema != null && schema.getFieldSchemaList() != null) {
            fields = schema.getFieldSchemaList().stream()
                    .map(f -> CollectionInfo.FieldInfo.builder()
                            .name(f.getName())
                            .dataType(f.getDataType() != null ? f.getDataType().name() : "UNKNOWN")
                            .primaryKey(f.getIsPrimaryKey() != null && f.getIsPrimaryKey())
                            .build())
                    .collect(Collectors.toList());
        }

        return CollectionInfo.builder()
                .name(desc.getCollectionName())
                .description(desc.getDescription())
                .rowCount(stats.getNumOfEntities() != null ? stats.getNumOfEntities() : 0L)
                .loaded(true)
                .fields(fields)
                .indexes(List.of())
                .build();
    }

    public void dropCollection() {
        log.info("Dropping collection: {}", collectionName);
        client.dropCollection(DropCollectionReq.builder()
                .collectionName(collectionName)
                .build());
        initialized = false;
    }

    public Map<String, Object> getStats() {
        GetCollectionStatsResp stats = client.getCollectionStats(GetCollectionStatsReq.builder()
                .collectionName(collectionName)
                .build());
        Map<String, Object> result = new HashMap<>();
        result.put("rowCount", stats.getNumOfEntities());
        result.put("collectionName", collectionName);
        return result;
    }

    // ==================== Data Operations ====================

    private void ensureInitialized() {
        if (!initialized) {
            log.warn("Milvus not initialized, lazy initializing");
            initWithRetry(3, 2000);
        }
    }

    public void insert(String id, float[] embedding, String content,
                        String documentName, String docId, int chunkIndex) {
        ensureInitialized();

        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        row.addProperty("content", content);
        row.addProperty("document_name", documentName != null ? documentName : "");
        row.addProperty("doc_id", docId != null ? docId : "");
        row.addProperty("chunk_index", chunkIndex);
        row.addProperty("created_at", System.currentTimeMillis() / 1000);

        JsonArray embArray = new JsonArray();
        for (float f : embedding) {
            embArray.add(f);
        }
        row.add("embedding", embArray);

        client.insert(InsertReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(row))
                .build());
    }

    public void batchInsert(List<InsertData> dataList) {
        ensureInitialized();
        if (dataList == null || dataList.isEmpty()) return;

        List<JsonObject> rows = new ArrayList<>();
        for (InsertData data : dataList) {
            JsonObject row = new JsonObject();
            row.addProperty("id", data.id);
            row.addProperty("content", data.content);
            row.addProperty("document_name", data.documentName != null ? data.documentName : "");
            row.addProperty("doc_id", data.docId != null ? data.docId : "");
            row.addProperty("chunk_index", data.chunkIndex);
            row.addProperty("created_at", System.currentTimeMillis() / 1000);

            JsonArray embArray = new JsonArray();
            for (float f : data.embedding) {
                embArray.add(f);
            }
            row.add("embedding", embArray);
            rows.add(row);
        }

        client.insert(InsertReq.builder()
                .collectionName(collectionName)
                .data(rows)
                .build());
    }

    // ==================== Dense Vector Search ====================

    public List<SearchResp.SearchResult> denseSearch(float[] queryEmbedding, int topK,
                                                      Map<String, String> filters) {
        ensureInitialized();

        List<Float> queryVector = new ArrayList<>();
        for (float f : queryEmbedding) {
            queryVector.add(f);
        }

        SearchReq.SearchReqBuilder<?, ?> builder = SearchReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .annsField("embedding")
                .topK(topK)
                .metricType(IndexParam.MetricType.COSINE)
                .outputFields(Arrays.asList("id", "content", "document_name", "doc_id"));

        if (filters != null && !filters.isEmpty()) {
            builder.filter(buildFilterExpression(filters));
        }

        SearchResp resp = client.search(builder.build());
        return unwrapResults(resp);
    }

    // ==================== Sparse/BM25 Search ====================

    public List<SearchResp.SearchResult> sparseSearch(String queryText, int topK) {
        ensureInitialized();

        SearchReq searchReq = SearchReq.builder()
                .collectionName(collectionName)
                .data(Collections.singletonList(new EmbeddedText(queryText)))
                .annsField("sparse_vec")
                .topK(topK)
                .metricType(IndexParam.MetricType.BM25)
                .outputFields(Arrays.asList("id", "content", "document_name", "doc_id"))
                .build();

        SearchResp resp = client.search(searchReq);
        return unwrapResults(resp);
    }

    // ==================== Hybrid Search ====================

    public List<SearchResp.SearchResult> hybridSearch(float[] queryEmbedding, String queryText,
                                                       int topK, float denseWeight, float sparseWeight,
                                                       int rrfK, boolean useRrf) {
        ensureInitialized();

        List<Float> queryVector = new ArrayList<>();
        for (float f : queryEmbedding) {
            queryVector.add(f);
        }

        AnnSearchReq denseReq = AnnSearchReq.builder()
                .vectorFieldName("embedding")
                .vectors(Collections.singletonList(new FloatVec(queryVector)))
                .topK(topK * 2)
                .metricType(IndexParam.MetricType.COSINE)
                .params("{\"ef\": 128}")
                .build();

        AnnSearchReq sparseReq = AnnSearchReq.builder()
                .vectorFieldName("sparse_vec")
                .vectors(Collections.singletonList(new EmbeddedText(queryText)))
                .topK(topK * 2)
                .metricType(IndexParam.MetricType.BM25)
                .params("{\"drop_ratio_search\": 0.2}")
                .build();

        HybridSearchReq.HybridSearchReqBuilder<?, ?> builder = HybridSearchReq.builder()
                .collectionName(collectionName)
                .searchRequests(Arrays.asList(denseReq, sparseReq))
                .topK(topK)
                .outFields(Arrays.asList("id", "content", "document_name", "doc_id"));

        if (useRrf) {
            builder.ranker(new RRFRanker(rrfK));
        } else {
            builder.ranker(new WeightedRanker(Arrays.asList(denseWeight, sparseWeight)));
        }

        SearchResp resp = client.hybridSearch(builder.build());
        return unwrapResults(resp);
    }

    // ==================== Helper Methods ====================

    private List<SearchResp.SearchResult> unwrapResults(SearchResp resp) {
        if (resp.getSearchResults() == null || resp.getSearchResults().isEmpty()) {
            return Collections.emptyList();
        }
        return resp.getSearchResults().get(0);
    }

    private String buildFilterExpression(Map<String, String> filters) {
        return filters.entrySet().stream()
                .map(e -> String.format("%s == \"%s\"", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" and "));
    }

    // ==================== Inner Class ====================

    public record InsertData(String id, float[] embedding, String content,
                             String documentName, String docId, int chunkIndex) {}
}
