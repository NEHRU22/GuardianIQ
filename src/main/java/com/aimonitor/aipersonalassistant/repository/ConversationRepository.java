
package com.aimonitor.aipersonalassistant.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aimonitor.aipersonalassistant.entity.Conversation;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    List<Conversation> findAllByUser_IdOrderByUpdatedAtDesc(
            Long userId
    );

    Optional<Conversation> findByIdAndUser_Id(
            Long conversationId,
            Long userId
    );
}
