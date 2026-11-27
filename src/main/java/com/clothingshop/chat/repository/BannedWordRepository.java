package com.clothingshop.chat.repository;

import com.clothingshop.chat.entity.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BannedWordRepository extends JpaRepository<BannedWord, Integer> {
    List<BannedWord> findByActiveTrueOrderByIdAsc();
    List<BannedWord> findAllByOrderByIdDesc();
    Optional<BannedWord> findByNormalizedWord(String normalizedWord);
}
