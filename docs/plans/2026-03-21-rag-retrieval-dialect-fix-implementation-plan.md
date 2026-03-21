# RAG Retrieval Service Dialect Fix Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an explicit Hibernate MySQL dialect to `rag-retrieval-service` so Spring Boot can initialize JPA even when JDBC metadata is unavailable during startup.

**Architecture:** This is a configuration-only fix. The implementation follows the working pattern already used by `doc-processing-service`, adding the minimum JPA configuration needed in `rag-retrieval-service` without changing Java code, dependencies, or unrelated settings.

**Tech Stack:** Spring Boot 3.4, Spring Data JPA, Hibernate 6, MySQL Connector/J, YAML configuration

---

## File Map

- Modify: `rag-knowledge-base/rag-retrieval-service/src/main/resources/application.yml` — add explicit Hibernate dialect under `spring.jpa.properties.hibernate`
- Reference: `rag-knowledge-base/doc-processing-service/src/main/resources/application.yml` — existing project pattern for explicit MySQL dialect
- Optional verification target: `rag-knowledge-base/rag-retrieval-service/pom.xml` — confirms JPA and MySQL dependencies already exist

## Chunk 1: Minimal configuration fix

### Task 1: Add the failing-startup reproduction note

**Files:**
- Modify: `docs/plans/2026-03-21-rag-retrieval-dialect-fix-design.md`

- [ ] **Step 1: Record the exact failing behavior**

Add a short note capturing the startup failure:

```text
org.springframework.beans.factory.BeanCreationException
Unable to determine Dialect without JDBC metadata
```

- [ ] **Step 2: Verify the note matches the observed failure**

Check that the recorded message matches the user-provided stack trace excerpt exactly.

- [ ] **Step 3: Do not implement code here**

No production change in this task. This is only to preserve the reproduction context.

### Task 2: Add the explicit Hibernate dialect

**Files:**
- Modify: `rag-knowledge-base/rag-retrieval-service/src/main/resources/application.yml`
- Reference: `rag-knowledge-base/doc-processing-service/src/main/resources/application.yml`

- [ ] **Step 1: Read the existing configuration and the working example**

Confirm `rag-retrieval-service` already contains:

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rag_knowledgebase?useUnicode=true&characterEncoding=utf-8
```

Confirm `doc-processing-service` already contains:

```yml
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

- [ ] **Step 2: Add the minimal configuration change**

Insert this block under `spring:` in `rag-retrieval-service/src/main/resources/application.yml`:

```yml
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

- [ ] **Step 3: Keep the change minimal**

Do not modify:
- datasource URL
- API key settings
- Milvus settings
- ports
- unrelated formatting

- [ ] **Step 4: Verify the YAML structure visually**

Ensure the file shape is:

```yml
spring:
  datasource:
    ...
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  ai:
    ...
```

### Task 3: Verify configuration correctness

**Files:**
- Modify: none
- Verify: `rag-knowledge-base/rag-retrieval-service/src/main/resources/application.yml`

- [ ] **Step 1: Re-read the modified file**

Confirm the dialect key is present exactly once.

- [ ] **Step 2: Confirm no duplicate `spring:` or `jpa:` blocks were introduced**

Expected result: one coherent YAML tree.

- [ ] **Step 3: Confirm the selected dialect value**

Expected value:

```text
org.hibernate.dialect.MySQLDialect
```

## Chunk 2: Optional runtime verification

### Task 4: Run targeted startup verification

**Files:**
- Modify: none
- Verify: `rag-knowledge-base/rag-retrieval-service`

- [ ] **Step 1: Run a targeted Spring Boot startup check**

Run from `rag-knowledge-base/rag-retrieval-service`:

```bash
mvn -s D:\maven\apache-maven-3.9.12\conf\settings.xml spring-boot:run
```

- [ ] **Step 2: Observe the result**

Expected outcomes:
- The original dialect error no longer appears, or
- A more specific database connectivity/authentication error appears

- [ ] **Step 3: Stop after collecting evidence**

This task is only for verification. Do not broaden scope into unrelated DB debugging unless requested.

## Completion criteria

- `rag-retrieval-service` has explicit Hibernate MySQL dialect configuration
- The YAML remains valid and minimal
- The original startup error is either removed or narrowed to a more specific datasource issue
- No unrelated files are changed

## Notes

- This fix is intentionally narrow and does not guarantee that MySQL connectivity is healthy.
- If startup still fails after this change, the next debugging step should focus on datasource connectivity rather than JPA dialect inference.
