# RAG Knowledge Base System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a RAG knowledge base system with 3 microservices (document processing, RAG retrieval, AI chat) and React frontend, enabling AI to answer questions based on uploaded documents.

**Architecture:** Microservices architecture with HTTP/REST communication. Document processing service handles upload/parsing/chunking. RAG retrieval service manages Milvus vector storage. AI chat service combines retrieval with generation. React frontend provides admin UI.

**Tech Stack:** Spring Boot 3.4 + Spring Cloud, React 18 + Ant Design 5 + Vite, Milvus, MySQL 8, MiniMax (Spring AI Alibaba), Apache Tika, EasyPoi, POI

---

## Prerequisites

1. Install Docker and Docker Compose
2. Install Java 17+
3. Install Node.js 18+
4. Start MySQL 8:
   ```bash
   docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=rag_knowledgebase mysql:8
   ```
5. Start Milvus (with etcd and MinIO via Docker Compose):
   ```bash
   docker-compose -f docker-compose-milvus.yml up -d
   ```

---

## Phase 1: Project Scaffolding

### Task 1: Create Project Structure

**Files:**
- Create: `rag-knowledge-base/`

**Step 1: Create root project directory**

```bash
mkdir -p rag-knowledge-base
cd rag-knowledge-base
```

**Step 2: Create service directories**

```bash
mkdir -p doc-processing-service/src/main/java/com/example/doc
mkdir -p doc-processing-service/src/main/resources
mkdir -p doc-processing-service/src/test/java/com/example/doc

mkdir -p rag-retrieval-service/src/main/java/com/example/rag
mkdir -p rag-retrieval-service/src/main/resources
mkdir -p rag-retrieval-service/src/test/java/com/example/rag

mkdir -p chat-service/src/main/java/com/example/chat
mkdir -p chat-service/src/main/resources
mkdir -p chat-service/src/test/java/com/example/chat

mkdir -p frontend/src/pages
mkdir -p frontend/src/components
mkdir -p frontend/src/services
mkdir -p frontend/public
```

**Step 3: Commit**

```bash
git init
git add .
git commit -m "chore: scaffold RAG knowledge base project structure"
```

---

### Task 2: Create Document Processing Service

**Files:**
- Create: `doc-processing-service/pom.xml`
- Create: `doc-processing-service/src/main/java/com/example/doc/DocApplication.java`
- Create: `doc-processing-service/src/main/resources/application.yml`

**Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>doc-processing-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>doc-processing-service</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
            <version>2.9.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
        <dependency>
            <groupId>cn.afterturn</groupId>
            <artifactId>easypoi-base</artifactId>
            <version>5.1.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.57</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

**Step 2: Create application.yml**

```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_knowledgebase?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB
```

**Step 3: Create main application class**

```java
package com.example.doc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocApplication {
    public static void main(String[] args) {
        SpringApplication.run(DocApplication.class, args);
    }
}
```

**Step 4: Commit**

```bash
git add doc-processing-service/
git commit -m "feat: create doc-processing-service scaffold"
```

---

### Task 3: Create RAG Retrieval Service

**Files:**
- Create: `rag-retrieval-service/pom.xml`
- Create: `rag-retrieval-service/src/main/java/com/example/rag/RagApplication.java`
- Create: `rag-retrieval-service/src/main/resources/application.yml`

**Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>rag-retrieval-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>rag-retrieval-service</name>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.1.2</spring-ai.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-minimax</artifactId>
        </dependency>
        <dependency>
            <groupId>io.milvus</groupId>
            <artifactId>milvus-sdk-java</artifactId>
            <version>2.3.3</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.57</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

**Step 2: Create application.yml**

```yaml
server:
  port: 8082
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_knowledgebase?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.com
milvus:
  host: localhost
  port: 19530
  collection: rag_chunks
  dimension: 1536
```

**Step 3: Create main application class**

```java
package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RagApplication {
    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}
```

**Step 4: Commit**

```bash
git add rag-retrieval-service/
git commit -m "feat: create rag-retrieval-service scaffold"
```

---

### Task 4: Create Chat Service

**Files:**
- Create: `chat-service/pom.xml`
- Create: `chat-service/src/main/java/com/example/chat/ChatApplication.java`
- Create: `chat-service/src/main/resources/application.yml`

**Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
    </parent>
    <groupId>com.example</groupId>
    <artifactId>chat-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>chat-service</name>

    <properties>
        <java.version>17</java.version>
        <spring-ai.version>1.1.2</spring-ai.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-minimax</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>2.0.57</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>
    </dependencies>
</project>
```

**Step 2: Create application.yml**

```yaml
server:
  port: 8083
spring:
  ai:
    minimax:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.com
      chat:
        options:
          model: MiniMax-M2.5-highspeed
```

**Step 3: Create main application class**

```java
package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
    }
}
```

**Step 4: Commit**

```bash
git add chat-service/
git commit -m "feat: create chat-service scaffold"
```

---

### Task 5: Create React Frontend

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.js`
- Create: `frontend/index.html`
- Create: `frontend/src/main.jsx`

**Step 1: Create package.json**

```json
{
  "name": "rag-frontend",
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "antd": "^5.12.0",
    "@ant-design/icons": "^5.2.6",
    "axios": "^1.6.2"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.2.1",
    "vite": "^5.0.8"
  }
}
```

**Step 2: Create vite.config.js**

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
```

**Step 3: Create index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <title>RAG 知识库管理</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>
```

**Step 4: Create src/main.jsx**

```jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
```

**Step 5: Commit**

```bash
git add frontend/
git commit -m "feat: create React frontend scaffold"
```

---

## Phase 2: Document Processing Service

### Task 6: Implement Document Parser

**Files:**
- Create: `doc-processing-service/src/main/java/com/example/doc/parser/DocumentParser.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/parser/impl/TxtParser.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/parser/impl/PdfParser.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/parser/impl/DocxParser.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/parser/impl/ExcelParser.java`

**Step 1: Create DocumentParser interface**

```java
package com.example.doc.parser;

public interface DocumentParser {
    String parse(byte[] content);
    boolean supports(String fileExtension);
}
```

**Step 2: Create TxtParser**

```java
package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.springframework.stereotype.Component;

@Component
public class TxtParser implements DocumentParser {

    @Override
    public String parse(byte[] content) {
        return new String(content);
    }

    @Override
    public boolean supports(String fileExtension) {
        return "txt".equalsIgnoreCase(fileExtension);
    }
}
```

**Step 3: Create PdfParser**

```java
package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class PdfParser implements DocumentParser {
    private final Tika tika = new Tika();

    @Override
    public String parse(byte[] content) {
        try {
            return tika.parseToString(new java.io.ByteArrayInputStream(content));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PDF", e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }
}
```

**Step 4: Create DocxParser**

```java
package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.List;

@Component
public class DocxParser implements DocumentParser {

    @Override
    public String parse(byte[] content) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             StringWriter writer = new StringWriter()) {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                writer.write(paragraph.getText());
                writer.write("\n");
            }
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse DOCX", e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "docx".equalsIgnoreCase(fileExtension);
    }
}
```

**Step 5: Create ExcelParser**

```java
package com.example.doc.parser.impl;

import com.example.doc.parser.DocumentParser;
import cn.afterturn.easypoi.excel.ExcelUtil;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
public class ExcelParser implements DocumentParser {

    @Override
    public String parse(byte[] content) {
        try (InputStream is = new ByteArrayInputStream(content)) {
            ImportParams params = new ImportParams();
            params.setHeadRows(1);
            List<Map<String, Object>> list = ExcelUtil.importExcel(is, Map.class, params);
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> row : list) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\t");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel", e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "xlsx".equalsIgnoreCase(fileExtension) || "xls".equalsIgnoreCase(fileExtension);
    }
}
```

**Step 6: Create DocumentParserFactory**

```java
package com.example.doc.parser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParserFactory {

    @Autowired
    private List<DocumentParser> parsers;

    public DocumentParser getParser(String fileExtension) {
        return parsers.stream()
                .filter(p -> p.supports(fileExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + fileExtension));
    }
}
```

**Step 7: Commit**

```bash
git add doc-processing-service/src/main/java/com/example/doc/parser/
git commit -m "feat(doc): implement document parsers for txt, pdf, docx, excel"
```

---

### Task 7: Implement Chunking Strategies

**Files:**
- Create: `doc-processing-service/src/main/java/com/example/doc/chunker/ChunkStrategy.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/chunker/TextChunk.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/chunker/impl/FixedLengthChunker.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/chunker/impl/SemanticChunker.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/chunker/impl/HybridChunker.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/chunker/impl/CustomRuleChunker.java`

**Step 1: Create TextChunk model**

```java
package com.example.doc.chunker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {
    private String content;
    private int index;
    private int startPos;
    private int endPos;
    private List<String> tags;
}
```

**Step 2: Create ChunkStrategy interface**

```java
package com.example.doc.chunker;

import java.util.List;

public interface ChunkStrategy {
    List<TextChunk> chunk(String text, ChunkConfig config);

    String getName();
}
```

**Step 3: Create ChunkConfig**

```java
package com.example.doc.chunker;

import lombok.Data;

@Data
public class ChunkConfig {
    private int chunkSize = 500;
    private int overlap = 50;
    private boolean keepHeaders = true;
    private int minParagraphLength = 50;
    private String[] delimiters = new String[]{"\n\n", "\n", ". "};
    private int[] headerLevels = new int[]{1, 2, 3};
}
```

**Step 4: Create FixedLengthChunker**

```java
package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedLengthChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getOverlap();

        for (int i = 0; i < text.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, text.length());
            String content = text.substring(i, end);
            chunks.add(TextChunk.builder()
                    .content(content)
                    .index(chunks.size())
                    .startPos(i)
                    .endPos(end)
                    .build());

            if (end == text.length()) break;
        }
        return chunks;
    }

    @Override
    public String getName() {
        return "fixed_length";
    }
}
```

**Step 5: Create SemanticChunker**

```java
package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SemanticChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();
        int currentPos = 0;

        for (String para : paragraphs) {
            if (para.trim().length() < config.getMinParagraphLength()) {
                continue;
            }

            if (currentChunk.length() + para.length() > config.getChunkSize()) {
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(chunks.size(), currentChunk.toString(), currentPos));
                    currentPos += currentChunk.length();
                    currentChunk = new StringBuilder();
                }
            }
            currentChunk.append(para).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(createChunk(chunks.size(), currentChunk.toString(), currentPos));
        }

        return chunks;
    }

    private TextChunk createChunk(int index, String content, int pos) {
        return TextChunk.builder()
                .content(content.trim())
                .index(index)
                .startPos(pos)
                .endPos(pos + content.length())
                .build();
    }

    @Override
    public String getName() {
        return "semantic";
    }
}
```

**Step 6: Create HybridChunker**

```java
package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HybridChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        // First do semantic chunking
        SemanticChunker semanticChunker = new SemanticChunker();
        List<TextChunk> semanticChunks = semanticChunker.chunk(text, config);

        List<TextChunk> result = new ArrayList<>();
        int index = 0;

        for (TextChunk chunk : semanticChunks) {
            if (chunk.getContent().length() > config.getChunkSize()) {
                // Do fixed-length chunking for oversized chunks
                FixedLengthChunker fixedChunker = new FixedLengthChunker();
                for (TextChunk subChunk : fixedChunker.chunk(chunk.getContent(), config)) {
                    subChunk.setIndex(index++);
                    result.add(subChunk);
                }
            } else {
                chunk.setIndex(index++);
                result.add(chunk);
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return "hybrid";
    }
}
```

**Step 7: Create CustomRuleChunker**

```java
package com.example.doc.chunker.impl;

import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CustomRuleChunker implements ChunkStrategy {

    @Override
    public List<TextChunk> chunk(String text, ChunkConfig config) {
        List<TextChunk> chunks = new ArrayList<>();
        String delimiter = config.getDelimiters()[0];
        String[] parts = text.split(Pattern.quote(delimiter));

        StringBuilder currentChunk = new StringBuilder();
        int currentPos = 0;

        for (String part : parts) {
            if (currentChunk.length() + part.length() > config.getChunkSize() && currentChunk.length() > 0) {
                chunks.add(TextChunk.builder()
                        .content(currentChunk.toString().trim())
                        .index(chunks.size())
                        .startPos(currentPos)
                        .endPos(currentPos + currentChunk.length())
                        .build());
                currentPos += currentChunk.length();
                currentChunk = new StringBuilder();
            }
            currentChunk.append(part).append(delimiter);
        }

        if (currentChunk.length() > 0) {
            chunks.add(TextChunk.builder()
                    .content(currentChunk.toString().trim())
                    .index(chunks.size())
                    .startPos(currentPos)
                    .endPos(currentPos + currentChunk.length())
                    .build());
        }

        return chunks;
    }

    @Override
    public String getName() {
        return "custom_rule";
    }
}
```

**Step 8: Commit**

```bash
git add doc-processing-service/src/main/java/com/example/doc/chunker/
git commit -m "feat(doc): implement all chunking strategies"
```

---

### Task 8: Implement Chunk Entity and Repository

**Files:**
- Create: `doc-processing-service/src/main/java/com/example/doc/entity/Chunk.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/repository/ChunkRepository.java`

**Step 1: Create Chunk entity**

```java
package com.example.doc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    private String strategy;

    @Column(columnDefinition = "JSON")
    private String tags;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

**Step 2: Create ChunkRepository**

```java
package com.example.doc.repository;

import com.example.doc.entity.Chunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, String> {

    Page<Chunk> findByDocumentNameContaining(String documentName, Pageable pageable);

    List<Chunk> findByDocumentName(String documentName);

    void deleteByDocumentName(String documentName);

    long countByDocumentName(String documentName);
}
```

**Step 3: Commit**

```bash
git add doc-processing-service/src/main/java/com/example/doc/entity/
git add doc-processing-service/src/main/java/com/example/doc/repository/
git commit -m "feat(doc): add Chunk entity and repository"
```

---

### Task 9: Implement Document and Chunk Controllers

**Files:**
- Create: `doc-processing-service/src/main/java/com/example/doc/dto/ChunkDto.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/dto/ChunkRequest.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/service/DocumentService.java`
- Create: `doc-processing-service/src/main/java/com/example/doc/controller/DocumentController.java`

**Step 1: Create ChunkDto**

```java
package com.example.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkDto {
    private String id;
    private String content;
    private String documentName;
    private Integer chunkSize;
    private Integer chunkIndex;
    private String strategy;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Step 2: Create ChunkRequest**

```java
package com.example.doc.dto;

import lombok.Data;

@Data
public class ChunkRequest {
    private String content;
    private String documentName;
    private String strategy;
    private Integer chunkSize;
    private Integer overlap;
    private Boolean keepHeaders;
    private Integer minParagraphLength;
    private String[] delimiters;
    private Integer[] headerLevels;
    private List<String> tags;
}
```

**Step 3: Create DocumentService**

```java
package com.example.doc.service;

import com.alibaba.fastjson.JSON;
import com.example.doc.chunker.ChunkConfig;
import com.example.doc.chunker.ChunkStrategy;
import com.example.doc.chunker.TextChunk;
import com.example.doc.dto.ChunkDto;
import com.example.doc.dto.ChunkRequest;
import com.example.doc.entity.Chunk;
import com.example.doc.parser.DocumentParser;
import com.example.doc.parser.DocumentParserFactory;
import com.example.doc.repository.ChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentService {

    private final DocumentParserFactory parserFactory;
    private final List<ChunkStrategy> chunkStrategies;
    private final ChunkRepository chunkRepository;
    private final WebClient ragServiceClient = WebClient.create("http://localhost:8082");

    public List<ChunkDto> uploadAndChunk(MultipartFile file, ChunkRequest request) {
        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName);

        DocumentParser parser = parserFactory.getParser(extension);
        String content;
        try {
            content = parser.parse(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse document", e);
        }

        ChunkConfig config = buildChunkConfig(request);
        ChunkStrategy strategy = getChunkStrategy(request.getStrategy());

        List<TextChunk> chunks = strategy.chunk(content, config);

        return chunks.stream().map(tc -> {
            Chunk chunk = Chunk.builder()
                    .id(UUID.randomUUID().toString())
                    .content(tc.getContent())
                    .documentName(fileName)
                    .chunkSize(tc.getContent().length())
                    .chunkIndex(tc.getIndex())
                    .strategy(strategy.getName())
                    .tags(JSON.toJSONString(tc.getTags() != null ? tc.getTags() : request.getTags()))
                    .build();
            chunkRepository.save(chunk);

            // 【关键修复】同步索引到 Milvus
            indexToVectorStore(chunk);

            return toDto(chunk);
        }).collect(Collectors.toList());
    }

    /**
     * 将 chunk 同步索引到 Milvus 向量数据库
     */
    private void indexToVectorStore(Chunk chunk) {
        try {
            ragServiceClient.post()
                    .uri("/api/vector/index")
                    .bodyValue(java.util.Map.of(
                            "id", chunk.getId(),
                            "content", chunk.getContent(),
                            "documentName", chunk.getDocumentName()
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to index chunk to vector store: " + chunk.getId(), e);
        }
    }

    public Page<ChunkDto> getChunks(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Chunk> chunkPage = keyword != null
                ? chunkRepository.findByDocumentNameContaining(keyword, pageRequest)
                : chunkRepository.findAll(pageRequest);
        return chunkPage.map(this::toDto);
    }

    public ChunkDto getChunk(String id) {
        return toDto(chunkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + id)));
    }

    public ChunkDto updateChunk(String id, ChunkRequest request) {
        Chunk chunk = chunkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + id));

        if (request.getContent() != null) chunk.setContent(request.getContent());
        if (request.getTags() != null) chunk.setTags(JSON.toJSONString(request.getTags()));

        chunkRepository.save(chunk);
        return toDto(chunk);
    }

    public void deleteChunk(String id) {
        chunkRepository.deleteById(id);
    }

    public void deleteChunks(List<String> ids) {
        chunkRepository.deleteAllById(ids);
    }

    private ChunkConfig buildChunkConfig(ChunkRequest request) {
        ChunkConfig config = new ChunkConfig();
        if (request.getChunkSize() != null) config.setChunkSize(request.getChunkSize());
        if (request.getOverlap() != null) config.setOverlap(request.getOverlap());
        if (request.getKeepHeaders() != null) config.setKeepHeaders(request.getKeepHeaders());
        if (request.getMinParagraphLength() != null) config.setMinParagraphLength(request.getMinParagraphLength());
        if (request.getDelimiters() != null) config.setDelimiters(request.getDelimiters());
        if (request.getHeaderLevels() != null) config.setHeaderLevels(request.getHeaderLevels());
        return config;
    }

    private ChunkStrategy getChunkStrategy(String strategyName) {
        return chunkStrategies.stream()
                .filter(s -> s.getName().equals(strategyName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown strategy: " + strategyName));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private ChunkDto toDto(Chunk chunk) {
        return ChunkDto.builder()
                .id(chunk.getId())
                .content(chunk.getContent())
                .documentName(chunk.getDocumentName())
                .chunkSize(chunk.getChunkSize())
                .chunkIndex(chunk.getChunkIndex())
                .strategy(chunk.getStrategy())
                .tags(JSON.parseArray(chunk.getTags(), String.class))
                .createdAt(chunk.getCreatedAt())
                .updatedAt(chunk.getUpdatedAt())
                .build();
    }
}
```

**Step 4: Create DocumentController**

```java
package com.example.doc.controller;

import com.example.doc.dto.ChunkDto;
import com.example.doc.dto.ChunkRequest;
import com.example.doc.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public List<ChunkDto> upload(@RequestParam("file") MultipartFile file,
                                  @RequestBody ChunkRequest request) {
        return documentService.uploadAndChunk(file, request);
    }

    @GetMapping("/chunks")
    public Page<ChunkDto> getChunks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return documentService.getChunks(page, size, keyword);
    }

    @GetMapping("/chunks/{id}")
    public ChunkDto getChunk(@PathVariable String id) {
        return documentService.getChunk(id);
    }

    @PutMapping("/chunks/{id}")
    public ChunkDto updateChunk(@PathVariable String id, @RequestBody ChunkRequest request) {
        return documentService.updateChunk(id, request);
    }

    @DeleteMapping("/chunks/{id}")
    public void deleteChunk(@PathVariable String id) {
        documentService.deleteChunk(id);
    }

    @DeleteMapping("/chunks/batch")
    public void deleteChunks(@RequestBody List<String> ids) {
        documentService.deleteChunks(ids);
    }
}
```

**Step 5: Commit**

```bash
git add doc-processing-service/src/main/java/com/example/doc/dto/
git add doc-processing-service/src/main/java/com/example/doc/service/
git add doc-processing-service/src/main/java/com/example/doc/controller/
git commit -m "feat(doc): implement document and chunk REST APIs"
```

---

## Phase 3: RAG Retrieval Service

### Task 10: Implement Milvus Integration

**Files:**
- Create: `rag-retrieval-service/src/main/java/com/example/rag/config/MilvusConfig.java`
- Create: `rag-retrieval-service/src/main/java/com/example/rag/service/EmbeddingService.java`
- Create: `rag-retrieval-service/src/main/java/com/example/rag/service/MilvusService.java`

**Step 1: Create MilvusConfig**

```java
package com.example.rag.config;

import io.milvus.client.MilvusClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private int port;

    @Bean
    public MilvusClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        return new MilvusClient(connectParam);
    }
}
```

**Step 2: Create EmbeddingService**

```java
package com.example.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ChatModel miniMaxChatModel;
    private final EmbeddingModel embeddingModel;

    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    public List<float[]> embed(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .toList();
    }

    public VectorStore buildVectorStore(List<String> texts, List<String> ids) {
        return MilvusVectorStore.builder(embeddingModel)
                .collectionName("rag_chunks")
                .build();
    }
}
```

**Step 3: Create MilvusService**

```java
package com.example.rag.service;

import com.example.rag.dto.SearchResult;
import io.milvus.client.MilvusClient;
import io.milvus.param.collection.CollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.grpc.SearchResults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilvusService {

    private final MilvusClient milvusClient;

    @Value("${milvus.collection}")
    private String collectionName;

    @Value("${milvus.dimension}")
    private int dimension;

    @PostConstruct
    public void initCollection() {
        try {
            milvusClient.describeCollection(CollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build());
            log.info("Collection {} already exists", collectionName);
        } catch (Exception e) {
            log.info("Creating collection: {}", collectionName);
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
        }
    }

    public void insert(String id, float[] embedding, String content, String documentName) {
        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("id", Collections.singletonList(id)),
                new InsertParam.Field("embedding", Collections.singletonList(embedding)),
                new InsertParam.Field("content", Collections.singletonList(content)),
                new InsertParam.Field("document_name", Collections.singletonList(documentName))
        );

        milvusClient.insert(InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build());
    }

    public List<SearchResult> search(float[] queryEmbedding, int topK) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(Collections.singletonList(queryEmbedding))
                .withTopK(topK)
                .withVectorFieldName("embedding")
                .build();

        SearchResults results = milvusClient.search(searchParam);

        List<SearchResult> searchResults = new ArrayList<>();
        for (io.milvus.grpc.SearchResult result : results.getResults().getResultsList()) {
            searchResults.add(SearchResult.builder()
                    .id(result.getIds().getIntId().getData(0))
                    .score(result.getScores(0))
                    .content(result.getFieldsDataList().stream()
                            .filter(f -> f.getFieldName().equals("content"))
                            .findFirst()
                            .map(f -> f.getScalars().getField(0).getData().getBinary())
                            .map(b -> new String(b))
                            .orElse(""))
                    .build());
        }
        return searchResults;
    }
}
```

**Step 4: Create SearchResult DTO**

```java
package com.example.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private long id;
    private float score;
    private String content;
    private String documentName;
}
```

**Step 5: Commit**

```bash
git add rag-retrieval-service/src/main/java/com/example/rag/config/
git add rag-retrieval-service/src/main/java/com/example/rag/service/
git add rag-retrieval-service/src/main/java/com/example/rag/dto/
git commit -m "feat(rag): implement Milvus integration and embedding service"
```

---

### Task 11: Implement Vector Store Controller

**Files:**
- Create: `rag-retrieval-service/src/main/java/com/example/rag/controller/VectorController.java`
- Create: `rag-retrieval-service/src/main/java/com/example/rag/service/VectorService.java`

**Step 1: Create VectorService**

```java
package com.example.rag.service;

import com.example.rag.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorService {

    private final EmbeddingService embeddingService;
    private final MilvusService milvusService;

    public void indexChunk(String id, String content, String documentName) {
        float[] embedding = embeddingService.embed(content);
        milvusService.insert(id, embedding, content, documentName);
    }

    public List<SearchResult> search(String query, int topK) {
        float[] queryEmbedding = embeddingService.embed(query);
        return milvusService.search(queryEmbedding, topK);
    }
}
```

**Step 2: Create VectorController**

```java
package com.example.rag.controller;

import com.example.rag.dto.SearchResult;
import com.example.rag.service.VectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VectorController {

    private final VectorService vectorService;

    @PostMapping("/index")
    public void indexChunk(@RequestBody Map<String, String> request) {
        vectorService.indexChunk(
                request.get("id"),
                request.get("content"),
                request.get("documentName")
        );
    }

    @PostMapping("/search")
    public List<SearchResult> search(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.containsKey("topK") ? (Integer) request.get("topK") : 5;
        return vectorService.search(query, topK);
    }
}
```

**Step 3: Commit**

```bash
git add rag-retrieval-service/src/main/java/com/example/rag/controller/
git commit -m "feat(rag): implement vector search API"
```

---

## Phase 4: Chat Service

### Task 12: Implement RAG Chat Service

**Files:**
- Create: `chat-service/src/main/java/com/example/chat/controller/ChatController.java`
- Create: `chat-service/src/main/java/com/example/chat/service/RagChatService.java`
- Create: `chat-service/src/main/java/com/example/chat/config/WebClientConfig.java`
- Create: `chat-service/src/main/java/com/example/chat/dto/ChatRequest.java`

**Step 1: Create ChatRequest DTO**

```java
package com.example.chat.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private int topK = 5;
    private boolean stream = true;
}
```

**Step 2: Create WebClientConfig**

```java
package com.example.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }
}
```

**Step 3: Create RagChatService**

```java
package com.example.chat.service;

import com.example.chat.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {

    private final ChatModel miniMaxChatModel;
    private final WebClient webClient;

    public Flux<String> chatStream(ChatRequest request) {
        // Call RAG retrieval service to get relevant chunks
        List<RetrievalResult> chunks = retrieveChunks(request.getMessage(), request.getTopK());

        // Build context from chunks
        String context = buildContext(chunks);

        // Build system prompt with context
        String systemPrompt = """
                You are a helpful AI assistant. Use the following context to answer the user's question.
                If the context doesn't contain relevant information, say so.

                Context:
                %s
                """.formatted(context);

        // Stream response
        return ChatClient.builder(miniMaxChatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(request.getMessage())
                .stream()
                .content();
    }

    private List<RetrievalResult> retrieveChunks(String query, int topK) {
        try {
            @SuppressWarnings("unchecked")
            List<RetrievalResult> results = webClient.post()
                    .uri("/api/vector/search")
                    .bodyValue(java.util.Map.of("query", query, "topK", topK))
                    .retrieve()
                    .bodyToMono((Class<List<RetrievalResult>>)(Class<?>)List.class)
                    .block();
            return results != null ? results : List.of();
        } catch (Exception e) {
            log.error("Failed to retrieve chunks", e);
            return List.of();
        }
    }

    private String buildContext(List<RetrievalResult> chunks) {
        if (chunks.isEmpty()) {
            return "No relevant information found in the knowledge base.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalResult chunk = chunks.get(i);
            sb.append("[%d] %s (source: %s, score: %.2f)\n"
                    .formatted(i + 1, chunk.getContent(), chunk.getDocumentName(), chunk.getScore()));
        }
        return sb.toString();
    }

    record RetrievalResult(String content, String documentName, float score) {}
}
```

**Step 4: Create ChatController**

```java
package com.example.chat.controller;

import com.example.chat.dto.ChatRequest;
import com.example.chat.service.RagChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final RagChatService ragChatService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return ragChatService.chatStream(request);
    }
}
```

**Step 5: Commit**

```bash
git add chat-service/src/main/java/com/example/chat/
git commit -m "feat(chat): implement RAG chat service with streaming"
```

---

## Phase 5: React Frontend

### Task 13: Implement API Service

**Files:**
- Create: `frontend/src/services/api.js`

**Step 1: Create API service**

```javascript
import axios from 'axios';

const API_BASE = 'http://localhost:8081/api';
const VECTOR_API = 'http://localhost:8082/api/vector';
const CHAT_API = 'http://localhost:8083/api/chat';

const docApi = axios.create({ baseURL: API_BASE });
const vectorApi = axios.create({ baseURL: VECTOR_API });
const chatApi = axios.create({ baseURL: CHAT_API });

export const documentApi = {
  upload: (file, config) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('request', new Blob([JSON.stringify(config)], { type: 'application/json' }));
    return docApi.post('/doc/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },

  getChunks: (page = 0, size = 10, keyword = '') => {
    return docApi.get('/doc/chunks', { params: { page, size, keyword } });
  },

  getChunk: (id) => docApi.get(`/doc/chunks/${id}`),

  updateChunk: (id, data) => docApi.put(`/doc/chunks/${id}`, data),

  deleteChunk: (id) => docApi.delete(`/doc/chunks/${id}`),

  deleteChunks: (ids) => docApi.delete('/doc/chunks/batch', { data: ids }),
};

export const vectorApi = {
  search: (query, topK = 5) => vectorApi.post('/vector/search', { query, topK }),
};

export const chatApi = {
  stream: (message, topK = 5) => {
    return fetch(`${CHAT_API}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, topK, stream: true }),
    });
  },
};
```

**Step 2: Commit**

```bash
git add frontend/src/services/api.js
git commit -m "feat(frontend): add API service layer"
```

---

### Task 14: Implement Document Upload Page

**Files:**
- Create: `frontend/src/pages/DocumentUpload.jsx`
- Create: `frontend/src/App.jsx`

**Step 1: Create DocumentUpload.jsx**

```jsx
import React, { useState } from 'react';
import { Upload, Button, Form, Select, InputNumber, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';

const { Dragger } = Upload;

const CHUNK_STRATEGIES = [
  { value: 'fixed_length', label: '固定长度' },
  { value: 'semantic', label: '语义拆分' },
  { value: 'hybrid', label: '混合拆分' },
  { value: 'custom_rule', label: '自定义规则' },
];

export default function DocumentUpload() {
  const [form] = Form.useForm();
  const [uploading, setUploading] = useState(false);

  const handleUpload = async (file) => {
    setUploading(true);
    try {
      const values = await form.validateFields();
      const { data } = await import('../services/api').then(m => m.documentApi.upload(file, values));
      message.success(`上传成功，已拆分为 ${data.length} 个知识块`);
    } catch (error) {
      message.error('上传失败: ' + (error.message || '未知错误'));
    } finally {
      setUploading(false);
    }
    return false; // Prevent default upload behavior
  };

  return (
    <div style={{ padding: 24 }}>
      <h2>文档上传</h2>
      <Form form={form} layout="vertical" initialValues={{ strategy: 'hybrid', chunkSize: 500, overlap: 50 }}>
        <Form.Item label="拆分策略" name="strategy">
          <Select options={CHUNK_STRATEGIES} />
        </Form.Item>

        <Form.Item label="块大小(字符数)" name="chunkSize">
          <InputNumber min={100} max={5000} />
        </Form.Item>

        <Form.Item label="重叠字符数" name="overlap">
          <InputNumber min={0} max={200} />
        </Form.Item>

        <Dragger accept=".txt,.pdf,.docx,.xlsx,.xls" beforeUpload={handleUpload} showUploadList={false}>
          <p className="ant-upload-drag-icon">
            <UploadOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p className="ant-upload-hint">支持 txt, pdf, docx, xlsx, xls 格式</p>
        </Dragger>

        <Button type="primary" icon={<UploadOutlined />} loading={uploading} style={{ marginTop: 16 }}>
          {uploading ? '处理中...' : '确认上传'}
        </Button>
      </Form>
    </div>
  );
}
```

**Step 2: Create App.jsx**

```jsx
import React from 'react';
import { Layout, Menu } from 'antd';
import { UploadOutlined, DatabaseOutlined, CommentOutlined } from '@ant-design/icons';
import DocumentUpload from './pages/DocumentUpload';
import ChunkManagement from './pages/ChunkManagement';
import ChatTest from './pages/ChatTest';

const { Header, Content } = Layout;

export default function App() {
  const [current, setCurrent] = React.useState('upload');

  const renderPage = () => {
    switch (current) {
      case 'upload': return <DocumentUpload />;
      case 'chunks': return <ChunkManagement />;
      case 'chat': return <ChatTest />;
      default: return <DocumentUpload />;
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header>
        <Menu mode="horizontal" selectedKeys={[current]} onClick={e => setCurrent(e.key)} theme="dark">
          <Menu.Item key="upload" icon={<UploadOutlined />}>文档上传</Menu.Item>
          <Menu.Item key="chunks" icon={<DatabaseOutlined />}>知识块管理</Menu.Item>
          <Menu.Item key="chat" icon={<CommentOutlined />}>问答测试</Menu.Item>
        </Menu>
      </Header>
      <Content style={{ padding: 24 }}>{renderPage()}</Content>
    </Layout>
  );
}
```

**Step 3: Commit**

```bash
git add frontend/src/pages/DocumentUpload.jsx frontend/src/App.jsx
git commit -m "feat(frontend): implement document upload page"
```

---

### Task 15: Implement Chunk Management Page

**Files:**
- Create: `frontend/src/pages/ChunkManagement.jsx`

**Step 1: Create ChunkManagement.jsx**

```jsx
import React, { useState, useEffect } from 'react';
import { Table, Button, Input, Space, Modal, message, Popconfirm } from 'antd';
import { SearchOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { documentApi } from '../services/api';

const { TextArea } = Input;

export default function ChunkManagement() {
  const [chunks, setChunks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [editingChunk, setEditingChunk] = useState(null);

  const fetchChunks = async () => {
    setLoading(true);
    try {
      const { data } = await documentApi.getChunks(page, 10, keyword);
      setChunks(data.content);
      setTotal(data.totalElements);
    } catch (error) {
      message.error('获取知识块失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchChunks();
  }, [page, keyword]);

  const handleDelete = async (id) => {
    try {
      await documentApi.deleteChunk(id);
      message.success('删除成功');
      fetchChunks();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleBatchDelete = async () => {
    const selectedIds = chunks.filter(c => c.selected).map(c => c.id);
    if (selectedIds.length === 0) {
      message.warning('请先选择要删除的知识块');
      return;
    }
    try {
      await documentApi.deleteChunks(selectedIds);
      message.success('批量删除成功');
      fetchChunks();
    } catch (error) {
      message.error('批量删除失败');
    }
  };

  const handleEdit = (record) => {
    setEditingChunk(record);
    setEditModalVisible(true);
  };

  const handleEditSave = async () => {
    try {
      await documentApi.updateChunk(editingChunk.id, { content: editingChunk.content });
      message.success('更新成功');
      setEditModalVisible(false);
      fetchChunks();
    } catch (error) {
      message.error('更新失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 200, ellipsis: true },
    { title: '文档名', dataIndex: 'documentName', width: 150 },
    { title: '内容', dataIndex: 'content', ellipsis: true },
    { title: '大小', dataIndex: 'chunkSize', width: 80 },
    { title: '策略', dataIndex: 'strategy', width: 100 },
    { title: '创建时间', dataIndex: 'createdAt', width: 180 },
    {
      title: '操作',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <h2>知识块管理</h2>

      <Space style={{ marginBottom: 16 }}>
        <Input.Search placeholder="搜索文档名" onSearch={setKeyword} style={{ width: 200 }} />
        <Button onClick={handleBatchDelete} danger>批量删除</Button>
      </Space>

      <Table
        columns={columns}
        dataSource={chunks}
        loading={loading}
        rowKey="id"
        pagination={{
          current: page + 1,
          total,
          onChange: (p) => setPage(p - 1),
        }}
      />

      <Modal title="编辑知识块" open={editModalVisible} onOk={handleEditSave} onCancel={() => setEditModalVisible(false)}>
        {editingChunk && (
          <TextArea rows={10} value={editingChunk.content} onChange={e => setEditingChunk({ ...editingChunk, content: e.target.value })} />
        )}
      </Modal>
    </div>
  );
}
```

**Step 2: Commit**

```bash
git add frontend/src/pages/ChunkManagement.jsx
git commit -m "feat(frontend): implement chunk management page"
```

---

### Task 16: Implement Chat Test Page

**Files:**
- Create: `frontend/src/pages/ChatTest.jsx`

**Step 1: Create ChatTest.jsx**

```jsx
import React, { useState } from 'react';
import { Input, Button, Card, Spin, message } from 'antd';
import { SendOutlined, RobotOutlined } from '@ant-design/icons';

const { TextArea } = Input;

export default function ChatTest() {
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState('');

  const handleSend = async () => {
    if (!message.trim()) return;
    setLoading(true);
    setResponse('');

    try {
      const response = await fetch('http://localhost:8083/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, topK: 5, stream: true }),
      });

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        setResponse(prev => prev + decoder.decode(value));
      }
    } catch (error) {
      message.error('发送失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <h2>问答测试</h2>

      <Card title="输入问题" style={{ marginBottom: 24 }}>
        <TextArea
          rows={4}
          value={message}
          onChange={e => setMessage(e.target.value)}
          placeholder="输入你的问题..."
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          loading={loading}
          style={{ marginTop: 16 }}
        >
          发送
        </Button>
      </Card>

      <Card
        title={<><RobotOutlined /> AI 回复</>}
        style={{ minHeight: 200 }}
      >
        {loading && <Spin />}
        {response && <div style={{ whiteSpace: 'pre-wrap' }}>{response}</div>}
        {!loading && !response && <div style={{ color: '#999' }}>AI 的回复将显示在这里</div>}
      </Card>
    </div>
  );
}
```

**Step 2: Update App.jsx to import ChatTest**

```jsx
import React from 'react';
import { Layout, Menu } from 'antd';
import { UploadOutlined, DatabaseOutlined, CommentOutlined } from '@ant-design/icons';
import DocumentUpload from './pages/DocumentUpload';
import ChunkManagement from './pages/ChunkManagement';
import ChatTest from './pages/ChatTest';

const { Header, Content } = Layout;

export default function App() {
  const [current, setCurrent] = React.useState('upload');

  const renderPage = () => {
    switch (current) {
      case 'upload': return <DocumentUpload />;
      case 'chunks': return <ChunkManagement />;
      case 'chat': return <ChatTest />;
      default: return <DocumentUpload />;
    }
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header>
        <Menu mode="horizontal" selectedKeys={[current]} onClick={e => setCurrent(e.key)} theme="dark">
          <Menu.Item key="upload" icon={<UploadOutlined />}>文档上传</Menu.Item>
          <Menu.Item key="chunks" icon={<DatabaseOutlined />}>知识块管理</Menu.Item>
          <Menu.Item key="chat" icon={<CommentOutlined />}>问答测试</Menu.Item>
        </Menu>
      </Header>
      <Content style={{ padding: 24 }}>{renderPage()}</Content>
    </Layout>
  );
}
```

**Step 3: Commit**

```bash
git add frontend/src/pages/ChatTest.jsx frontend/src/App.jsx
git commit -m "feat(frontend): implement chat test page"
```

---

## Phase 6: Integration Testing

### Task 17: Integration Test

**Step 1: Start all services**

```bash
# Terminal 1: Start Milvus
docker run -d --name milvus -p 19530:19530 milvusdb/milvus:v2.3.3

# Terminal 2: Start MySQL
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=rag_knowledgebase mysql:8

# Terminal 3: Start document service
cd doc-processing-service && mvn spring-boot:run

# Terminal 4: Start RAG retrieval service
cd rag-retrieval-service && mvn spring-boot:run

# Terminal 5: Start chat service
cd chat-service && mvn spring-boot:run

# Terminal 6: Start frontend
cd frontend && npm install && npm run dev
```

**Step 2: Test upload and chunk**

```bash
# Upload a test file
curl -X POST http://localhost:8081/api/doc/upload \
  -F "file=@test.txt" \
  -F "request={\"strategy\":\"hybrid\",\"chunkSize\":500}"
```

**Step 3: Test chat**

```bash
curl -X POST http://localhost:8083/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello","topK":5}'
```

**Step 4: Commit**

```bash
git add -A
git commit -m "chore: complete RAG knowledge base system v1.0"
```

---

## Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-5 | Project scaffolding |
| 2 | 6-9 | Document processing service |
| 3 | 10-11 | RAG retrieval service |
| 4 | 12 | Chat service |
| 5 | 13-16 | React frontend |
| 6 | 17 | Integration testing |

---

**Plan complete and saved to `docs/plans/2026-03-19-rag-knowledge-base-implementation-plan.md`.**

Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

Which approach?
