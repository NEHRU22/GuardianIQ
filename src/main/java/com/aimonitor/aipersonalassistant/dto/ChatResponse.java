
package com.aimonitor.aipersonalassistant.dto;

import java.time.LocalDateTime;

public class ChatResponse {

    private Long conversationId;
    private String conversationTitle;
    private Long messageId;
    private String userMessage;
    private String assistantMessage;
    private LocalDateTime createdAt;

    public ChatResponse() {
    }

    public ChatResponse(
            Long conversationId,
            String conversationTitle,
            Long messageId,
            String userMessage,
            String assistantMessage,
            LocalDateTime createdAt) {
        this.conversationId = conversationId;
        this.conversationTitle = conversationTitle;
        this.messageId = messageId;
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.createdAt = createdAt;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public Long getMessageId() {
        return messageId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
