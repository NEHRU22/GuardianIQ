package com.aimonitor.aipersonalassistant.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.aimonitor.aipersonalassistant.dto.ConversationSummaryResponse;
import com.aimonitor.aipersonalassistant.dto.MessageHistoryResponse;
import com.aimonitor.aipersonalassistant.entity.ChatMessage;
import com.aimonitor.aipersonalassistant.entity.Conversation;
import com.aimonitor.aipersonalassistant.entity.User;
import com.aimonitor.aipersonalassistant.repository.ChatMessageRepository;
import com.aimonitor.aipersonalassistant.repository.ConversationRepository;
import com.aimonitor.aipersonalassistant.repository.UserRepository;

@Service
public class ChatHistoryService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatHistoryService(
            UserRepository userRepository,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getConversations(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User account not found"
                ));

        List<Conversation> conversations =
                conversationRepository
                        .findAllByUser_IdOrderByUpdatedAtDesc(user.getId());

        return conversations.stream()
                .map(conversation -> new ConversationSummaryResponse(
                        conversation.getId(),
                        conversation.getTitle(),
                        conversation.getCreatedAt(),
                        conversation.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageHistoryResponse> getMessages(
            String email,
            Long conversationId) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User account not found"
                ));

        // Verify ownership before retrieving any messages.
        conversationRepository
                .findByIdAndUser_Id(conversationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Conversation not found"
                ));

        List<ChatMessage> messages =
                chatMessageRepository
                        .findAllByConversation_IdAndConversation_User_IdOrderByCreatedAtAsc(
                                conversationId,
                                user.getId()
                        );

        return messages.stream()
                .map(message -> new MessageHistoryResponse(
                        message.getId(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getCreatedAt()
                ))
                .toList();
    }
}