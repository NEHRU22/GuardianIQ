package com.aimonitor.aipersonalassistant.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aimonitor.aipersonalassistant.entity.ChatMessage;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage>
    findAllByConversation_IdAndConversation_User_IdOrderByCreatedAtAsc(
            Long conversationId,
            Long userId
    );

    @Query("""
        SELECT m
        FROM ChatMessage m
        WHERE m.conversation.user.id = :userId
          AND m.role = :role
          AND m.createdAt >= :startTime
          AND m.createdAt < :endTime
        ORDER BY m.createdAt ASC
        """)
    List<ChatMessage> findDailyMessages(
            @Param("userId") Long userId,
            @Param("role") ChatMessage.Role role,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}