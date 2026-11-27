package com.clothingshop.chat.repository;

import com.clothingshop.chat.entity.ChatViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatViolationRepository extends JpaRepository<ChatViolation, Long> {
    List<ChatViolation> findTop50ByOrderByIdDesc();
}
