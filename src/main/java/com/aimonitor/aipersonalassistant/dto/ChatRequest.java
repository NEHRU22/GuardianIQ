package com.aimonitor.aipersonalassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    private Long conversationId;

    @NotBlank(message = "Message cannot be empty")
    @Size(
            max = 5000,
            message = "Message cannot exceed 5000 characters"
    )
    private String message;

    @Size(max = 100, message = "Invalid document ID")
    private String documentId;

    public ChatRequest() {
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}