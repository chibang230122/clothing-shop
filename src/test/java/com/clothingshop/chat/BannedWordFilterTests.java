package com.clothingshop.chat;

import com.clothingshop.chat.entity.BannedWord;
import com.clothingshop.chat.entity.BannedWord.FilterAction;
import com.clothingshop.chat.repository.BannedWordRepository;
import com.clothingshop.chat.repository.ChatViolationRepository;
import com.clothingshop.chat.service.BannedWordFilterService;
import com.clothingshop.chat.service.BannedWordFilterService.FilterOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BannedWordFilterTests {

    private BannedWordFilterService filterService;

    @BeforeEach
    void setUp() {
        BannedWordRepository wordRepo = Mockito.mock(BannedWordRepository.class);
        ChatViolationRepository violRepo = Mockito.mock(ChatViolationRepository.class);

        BannedWord w1 = new BannedWord();
        w1.setWord("đồ ngốc");
        w1.setNormalizedWord("do ngoc");
        w1.setAction(FilterAction.MASK);

        BannedWord w2 = new BannedWord();
        w2.setWord("lừa đảo");
        w2.setNormalizedWord("lua dao");
        w2.setAction(FilterAction.BLOCK);

        BannedWord w3 = new BannedWord();
        w3.setWord("ngu");
        w3.setNormalizedWord("ngu");
        w3.setAction(FilterAction.MASK);

        when(wordRepo.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(w1, w2, w3));
        filterService = new BannedWordFilterService(wordRepo, violRepo);
        filterService.reloadCache();
    }

    @Test
    void shouldMaskVietnameseDiacriticsWhilePreservingSentence() {
        FilterOutcome out = filterService.evaluateAndLog(1, 10L, "Shop này đúng là đồ ngốc thật sự!");
        assertFalse(out.blocked());
        assertTrue(out.masked());
        assertEquals("Shop này đúng là *** thật sự!", out.filteredText());
    }

    @Test
    void shouldBlockSeverePhrasesCompletely() {
        FilterOutcome out = filterService.evaluateAndLog(1, 10L, "Cảnh báo trang này LỪA ĐẢO khách hàng");
        assertTrue(out.blocked());
        assertTrue(out.matchedWords().contains("lừa đảo"));
    }

    @Test
    void shouldNotFalsePositiveOnSubstringsInsideValidWords() {
        FilterOutcome out = filterService.evaluateAndLog(1, 10L, "Mọi người mặc áo này rất đẹp");
        assertFalse(out.blocked());
        assertFalse(out.masked());
        assertEquals("Mọi người mặc áo này rất đẹp", out.filteredText());
    }
}