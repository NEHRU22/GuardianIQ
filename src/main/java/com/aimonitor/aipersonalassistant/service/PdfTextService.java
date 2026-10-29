package com.aimonitor.aipersonalassistant.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 200;

    private final ApachePdfBoxDocumentParser parser =
            new ApachePdfBoxDocumentParser();

    public List<String> extractChunks(MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload a non-empty PDF.");
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "Only PDF files are supported.");
        }

        Document document;

        try (InputStream inputStream = file.getInputStream()) {
            document = parser.parse(inputStream);
        }

        String text = document.text()
                .replaceAll("\\s+", " ")
                .trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(
                    "No extractable text found in this PDF. " +
                    "Scanned PDFs may require OCR.");
        }

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());

            // Prefer splitting at a word boundary.
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);

                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == text.length()) {
                break;
            }

            start = Math.max(start + 1, end - CHUNK_OVERLAP);
        }

        return chunks;
    }
}