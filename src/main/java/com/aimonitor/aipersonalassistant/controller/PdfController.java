package com.aimonitor.aipersonalassistant.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import com.aimonitor.aipersonalassistant.service.PdfTextService;
import com.aimonitor.aipersonalassistant.service.PineconeRagService;

@RestController
@RequestMapping("/api/pdfs")
public class PdfController {

    private final PdfTextService pdfTextService;
    private final PineconeRagService pineconeRagService;

    public PdfController(
            PdfTextService pdfTextService,
            PineconeRagService pineconeRagService) {
        this.pdfTextService = pdfTextService;
        this.pineconeRagService = pineconeRagService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please log in first."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please select a PDF file."));
        }

        if (file.getSize() > 10L * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum PDF size is 10 MB."));
        }

        try {
            List<String> chunks = pdfTextService.extractChunks(file);

            String documentId = UUID.randomUUID().toString();

            // Give each authenticated user a separate namespace.
            String namespace = "user-" + authentication.getName()
                    .replaceAll("[^A-Za-z0-9_-]", "_");

            String filename = file.getOriginalFilename();

            pineconeRagService.storeChunks(
                    namespace,
                    documentId,
                    filename == null ? "uploaded.pdf" : filename,
                    chunks
            );

            return ResponseEntity.ok(Map.of(
                    "message", "PDF indexed successfully.",
                    "documentId", documentId,
                    "chunksStored", chunks.size()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Could not read the PDF."));

        } catch (org.springframework.web.client.RestClientResponseException e) {
    System.err.println("Pinecone HTTP status: " + e.getStatusCode());
    System.err.println("Pinecone response: " + e.getResponseBodyAsString());

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of(
                    "error",
                    "Pinecone indexing failed. Check the server terminal."
            ));

} catch (RestClientException e) {
    System.err.println("Pinecone connection error:");
    e.printStackTrace();

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of(
                    "error",
                    "Could not connect to Pinecone. Check the server terminal."
            ));
}
            }
        }