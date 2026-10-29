package com.aimonitor.aipersonalassistant.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PineconeRagService {

    private static final String API_VERSION = "2026-07";
    private static final int BATCH_SIZE = 50;

    private final RestClient pineconeRestClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PineconeRagService(RestClient pineconeRestClient) {
        this.pineconeRestClient = pineconeRestClient;
    }

    public void storeChunks(
            String namespace,
            String documentId,
            String filename,
            List<String> chunks) {

        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException(
                    "Namespace cannot be empty.");
        }

        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Document ID cannot be empty.");
        }

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Filename cannot be empty.");
        }

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No text chunks provided.");
        }

        for (int start = 0; start < chunks.size(); start += BATCH_SIZE) {
            List<Map<String, Object>> documents = new ArrayList<>();

            int end = Math.min(start + BATCH_SIZE, chunks.size());

            for (int i = start; i < end; i++) {
                String chunk = chunks.get(i);

                if (chunk == null || chunk.isBlank()) {
                    continue;
                }

                Map<String, Object> document = new LinkedHashMap<>();
                document.put("_id", documentId + "#" + i);
                document.put("chunk_text", chunk);
                document.put("title", filename);
                document.put("source", filename);
                document.put("document_id", documentId);
                document.put("chunk_index", i);

                documents.add(document);
            }

            if (documents.isEmpty()) {
                continue;
            }

            Map<String, Object> requestBody =
                    Map.of("documents", documents);

            pineconeRestClient.post()
                    .uri(
                            "/namespaces/{namespace}/documents/upsert",
                            namespace
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Pinecone-API-Version", API_VERSION)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    /**
     * Search all indexed PDF chunks in this user's namespace.
     */
    public List<String> searchRelevantChunks(
            String namespace,
            String question,
            int topK) {

        return searchRelevantChunks(
                namespace, question, topK, null
        );
    }

    /**
     * Search the selected PDF when documentId is supplied.
     */
    public List<String> searchRelevantChunks(
            String namespace,
            String question,
            int topK,
            String documentId) {

        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException(
                    "Namespace cannot be empty.");
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException(
                    "Question cannot be empty.");
        }

        if (topK < 1 || topK > 10000) {
            throw new IllegalArgumentException(
                    "topK must be between 1 and 10000.");
        }

        Map<String, Object> scoringMethod = Map.of(
                "type", "embed",
                "field", "chunk_text",
                "query", question
        );

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("score_by", List.of(scoringMethod));
        requestBody.put("top_k", topK);
        requestBody.put(
                "include_fields",
                List.of(
                        "chunk_text",
                        "title",
                        "source",
                        "document_id",
                        "chunk_index"
                )
        );

        if (documentId != null && !documentId.isBlank()) {
            requestBody.put(
                    "filter",
                    Map.of(
                            "document_id",
                            Map.of("$eq", documentId)
                    )
            );
        }

        // Read the response as text so we can parse it explicitly.
        String responseBody = pineconeRestClient.post()
                .uri(
                        "/namespaces/{namespace}/documents/search",
                        namespace
                )
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Pinecone-API-Version", API_VERSION)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return List.of();
        }

        final JsonNode response;

        try {
            response = objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not parse Pinecone search response.",
                    exception
            );
        }

        // Support the response shapes encountered by the Documents API.
        JsonNode matches = response.path("result").path("hits");

        if (!matches.isArray() || matches.size() == 0) {
            JsonNode alternative = response.path("matches");

            if (alternative.isArray()) {
                matches = alternative;
            }
        }

        if (!matches.isArray() || matches.size() == 0) {
            JsonNode alternative =
                    response.path("result").path("matches");

            if (alternative.isArray()) {
                matches = alternative;
            }
        }

        if (!matches.isArray() || matches.size() == 0) {
            JsonNode alternative = response.path("hits");

            if (alternative.isArray()) {
                matches = alternative;
            }
        }

        List<String> results = new ArrayList<>();

        for (JsonNode match : matches) {
            JsonNode fields = match.path("fields");

            if (!fields.isObject() || fields.isEmpty()) {
                fields = match;
            }

            String text = fields.path("chunk_text").asText("");

            if (text.isBlank()) {
                continue;
            }

            String source = fields.path("source")
                    .asText(
                            fields.path("title")
                                    .asText("Unknown PDF")
                    );

            results.add("Source: " + source + "\n" + text);
        }

        System.out.println(
                "[RAG] Pinecone matches: " + matches.size()
        );

        System.out.println(
                "[RAG] Usable PDF chunks: " + results.size()
        );

        return results;
    }
}