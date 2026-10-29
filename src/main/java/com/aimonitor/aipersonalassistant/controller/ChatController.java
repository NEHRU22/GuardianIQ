package com.aimonitor.aipersonalassistant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aimonitor.aipersonalassistant.dto.ChatRequest;
import com.aimonitor.aipersonalassistant.dto.ChatResponse;
import com.aimonitor.aipersonalassistant.dto.ConversationSummaryResponse;
import com.aimonitor.aipersonalassistant.dto.MessageHistoryResponse;
import com.aimonitor.aipersonalassistant.service.ChatHistoryService;
import com.aimonitor.aipersonalassistant.service.ChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;

    public ChatController(
            ChatService chatService,
            ChatHistoryService chatHistoryService) {
        this.chatService = chatService;
        this.chatHistoryService = chatHistoryService;
    }

    // Send a message to the AI.
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {

        ChatResponse response = chatService.sendMessage(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(response);
    }

    // Retrieve this user's conversations, newest first.
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryResponse>> getConversations(
            Authentication authentication) {

        return ResponseEntity.ok(
                chatHistoryService.getConversations(authentication.getName())
        );
    }

    // Retrieve saved messages for one conversation.
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageHistoryResponse>> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {

        return ResponseEntity.ok(
                chatHistoryService.getMessages(
                        authentication.getName(),
                        conversationId
                )
        );
    }
}