package com.example.rag.service;

import com.example.rag.dto.SearchResult;
import com.example.rag.entity.Chunk;
import com.example.rag.repository.ChunkRepository;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DescribeCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.grpc.SearchResultData;
import io.milvus.grpc.DescribeCollectionResponse;
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

    private final MilvusServiceClient milvusClient;
    private final ChunkRepository chunkRepository;

    @Value("${milvus.collection}")
    private String collectionName;

    @Value("${milvus.dimension}")
    private int dimension;

    private volatile boolean initialized = false;

    @PostConstruct
    public void initCollection() {
        initWithRetry(10, 3000); // Retry 10 times with 3s initial delay (exponential backoff)
    }

    private void initWithRetry(int maxRetries, long initialDelayMs) {
        int attempt = 0;
        long delayMs = initialDelayMs;

        while (attempt < maxRetries) {
            attempt++;
            try {
                log.info("Attempting to connect to Milvus (attempt {}/{})", attempt, maxRetries);

                // Try to create collection directly - will succeed if it doesn't exist
                // If it already exists, we'll get an exception and we just mark initialized = true
                createCollection();
                log.info("Collection {} created successfully", collectionName);
                initialized = true;
                return;
            } catch (Exception e) {
                // Collection might already exist, or other error like "index not found" (collection exists but not loaded)
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("already exists") || msg.contains("index not found")) {
                    log.info("Collection {} already exists or index not found, loading it", collectionName);
                    loadCollectionIfNeeded();
                    initialized = true;
                    return;
                }
                log.warn("Failed to initialize Milvus (attempt {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        log.info("Waiting {} ms before retry...", delayMs);
                        Thread.sleep(delayMs);
                        delayMs *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Failed to initialize Milvus after {} attempts", maxRetries);
    }

    private void createCollection() {
        log.info("Creating collection: {}", collectionName);

        // Drop existing collection if it exists (might have wrong dimension)
        try {
            milvusClient.dropCollection(DropCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            log.info("Dropped existing collection: {}", collectionName);
        } catch (Exception e) {
            log.info("No existing collection to drop: {}", e.getMessage());
        }

        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .addFieldType(FieldType.newBuilder()
                        .withName("id")
                        .withDataType(io.milvus.grpc.DataType.VarChar)
                        .withMaxLength(36)
                        .withPrimaryKey(true)
                        .build())
                .addFieldType(FieldType.newBuilder()
                        .withName("embedding")
                        .withDataType(io.milvus.grpc.DataType.FloatVector)
                        .withDimension(dimension)
                        .build())
                .addFieldType(FieldType.newBuilder()
                        .withName("content")
                        .withDataType(io.milvus.grpc.DataType.VarChar)
                        .withMaxLength(65535)
                        .build())
                .addFieldType(FieldType.newBuilder()
                        .withName("document_name")
                        .withDataType(io.milvus.grpc.DataType.VarChar)
                        .withMaxLength(255)
                        .build())
                .build();
        milvusClient.createCollection(param);
        log.info("Collection {} created successfully", collectionName);

        // Create index on the embedding field for efficient search
        log.info("Creating index on collection: {}", collectionName);
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(IndexType.FLAT)
                .withMetricType(MetricType.L2)
                .build();
        milvusClient.createIndex(indexParam);
        log.info("Index created successfully");

        // Flush to ensure all data is persisted
        milvusClient.flush(FlushParam.newBuilder()
                .withCollectionNames(Arrays.asList(collectionName))
                .build());
        log.info("Collection {} flushed", collectionName);

        // Load collection into memory for searching
        log.info("Loading collection: {}", collectionName);
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        log.info("Collection {} loaded successfully", collectionName);
    }

    private void loadCollectionIfNeeded() {
        try {
            DescribeCollectionParam describeParam = DescribeCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build();
            milvusClient.describeCollection(describeParam);
            log.info("Collection {} exists, ensuring it's loaded", collectionName);

            try {
                milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build());
            } catch (Exception loadEx) {
                // If load fails due to "index not found", create index first
                if (loadEx.getMessage() != null && loadEx.getMessage().contains("index not found")) {
                    log.warn("Collection {} needs index, creating now", collectionName);
                    CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFieldName("embedding")
                            .withIndexType(IndexType.FLAT)
                            .withMetricType(MetricType.L2)
                            .build();
                    milvusClient.createIndex(indexParam);
                    log.info("Index created for {}", collectionName);

                    // Flush and load
                    milvusClient.flush(FlushParam.newBuilder()
                            .withCollectionNames(Arrays.asList(collectionName))
                            .build());

                    milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());
                    log.info("Collection {} loaded after index creation", collectionName);
                } else {
                    throw loadEx;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load collection: {}", e.getMessage());
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            log.warn("Milvus not initialized, attempting lazy initialization");
            initWithRetry(3, 2000);
        }
    }

    public void insert(String id, float[] embedding, String content, String documentName) {
        ensureInitialized();

        // Convert float[] to List<Float>
        List<Float> embeddingList = new ArrayList<>();
        for (float f : embedding) {
            embeddingList.add(f);
        }

        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("id", Collections.singletonList(id)),
                new InsertParam.Field("embedding", Collections.singletonList(embeddingList)),
                new InsertParam.Field("content", Collections.singletonList(content)),
                new InsertParam.Field("document_name", Collections.singletonList(documentName))
        );

        milvusClient.insert(InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build());

        // Flush to ensure data is persisted
        milvusClient.flush(FlushParam.newBuilder()
                .withCollectionNames(Arrays.asList(collectionName))
                .build());
    }

    public List<SearchResult> search(float[] queryEmbedding, int topK) {
        ensureInitialized();

        // Convert float[] to List<Float>
        List<Float> queryVector = new ArrayList<>();
        for (float f : queryEmbedding) {
            queryVector.add(f);
        }

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(Collections.singletonList(queryVector))
                .withTopK(topK)
                .withVectorFieldName("embedding")
                .build();

        var response = milvusClient.search(searchParam);
        var results = response.getData();

        List<SearchResult> searchResults = new ArrayList<>();

        try {
            SearchResultData resultData = results.getResults();
            int count = (int) resultData.getIds().getStrId().getDataCount();

            // Collect all IDs to query from MySQL
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                ids.add(resultData.getIds().getStrId().getData(i));
            }

            // Fetch content from MySQL
            Map<String, Chunk> chunkMap = chunkRepository.findByIdIn(ids).stream()
                    .collect(Collectors.toMap(Chunk::getId, c -> c));

            for (int i = 0; i < count; i++) {
                String id = resultData.getIds().getStrId().getData(i);
                float score = resultData.getScores(i);

                // Get content from MySQL
                Chunk chunk = chunkMap.get(id);
                String content = chunk != null ? chunk.getContent() : "";
                String documentName = chunk != null ? chunk.getDocumentName() : "";

                searchResults.add(SearchResult.builder()
                        .id(id)
                        .score(score)
                        .content(content)
                        .documentName(documentName)
                        .build());
            }
        } catch (Exception e) {
            log.error("Error parsing search results", e);
        }

        return searchResults;
    }
}
