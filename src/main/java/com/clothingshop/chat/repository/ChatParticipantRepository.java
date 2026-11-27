package com.clothingshop.chat.repository;

import com.clothingshop.chat.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByConversationId(Long conversationId);
    Optional<ChatParticipant> findByConversationIdAndUserId(Long conversationId, Integer userId);
}
