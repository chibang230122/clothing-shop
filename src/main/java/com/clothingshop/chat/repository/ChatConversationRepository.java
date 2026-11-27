package com.clothingshop.chat.repository;

import com.clothingshop.chat.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    Optional<ChatConversation> findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(Integer customerId, String status);
    List<ChatConversation> findAllByOrderByUpdatedAtDesc();
}
