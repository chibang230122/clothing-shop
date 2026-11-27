package com.clothingshop.chat.service;

import com.clothingshop.chat.entity.BannedWord;
import com.clothingshop.chat.entity.BannedWord.FilterAction;
import com.clothingshop.chat.entity.ChatViolation;
import com.clothingshop.chat.repository.BannedWordRepository;
import com.clothingshop.chat.repository.ChatViolationRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BannedWordFilterService {

    public record FilterOutcome(
        boolean blocked,
        boolean masked,
        String filteredText,
        List<String> matchedWords
    ) {}

    private static final class CompiledRule {
        final BannedWord entity;
        final Pattern boundaryPattern;

        CompiledRule(BannedWord entity) {
            this.entity = entity;
            String escaped = Pattern.quote(entity.getNormalizedWord());
            this.boundaryPattern = Pattern.compile("(?<![a-z0-9])" + escaped + "(?![a-z0-9])");
        }
    }

    private final BannedWordRepository bannedWordRepository;
    private final ChatViolationRepository violationRepository;
    private final List<CompiledRule> activeRules = new CopyOnWriteArrayList<>();

    public BannedWordFilterService(BannedWordRepository bannedWordRepository,
                                   ChatViolationRepository violationRepository) {
        this.bannedWordRepository = bannedWordRepository;
        this.violationRepository = violationRepository;
    }

    @PostConstruct
    public void reloadCache() {
        List<CompiledRule> fresh = new ArrayList<>();
        for (BannedWord bw : bannedWordRepository.findByActiveTrueOrderByIdAsc()) {
            if (bw.getNormalizedWord() != null && !bw.getNormalizedWord().isBlank()) {
                fresh.add(new CompiledRule(bw));
            }
        }
        activeRules.clear();
        activeRules.addAll(fresh);
    }

    public static String normalizeText(String input) {
        if (input == null || input.isBlank()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            sb.append(foldSingleChar(input.charAt(i)));
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static String foldSingleChar(char c) {
        if (c == '\u0111' || c == '\u0110') return "d";
        String nfd = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        if (nfd.isEmpty()) return "";
        char base = nfd.charAt(0);
        return switch (base) {
            case '@' -> "a";
            case '0' -> "o";
            case '1', '!' -> "i";
            case '3' -> "e";
            case '$' -> "s";
            default -> Character.isLetterOrDigit(base) ? String.valueOf(base) : " ";
        };
    }

    @Transactional
    public FilterOutcome evaluateAndLog(Integer userId, Long conversationId, String rawContent) {
        if (rawContent == null || rawContent.isEmpty()) {
            return new FilterOutcome(false, false, "", List.of());
        }

        StringBuilder normBuilder = new StringBuilder();
        List<Integer> normToOrigIndex = new ArrayList<>();
        boolean lastWasSpace = true;

        for (int i = 0; i < rawContent.length(); i++) {
            String folded = foldSingleChar(rawContent.charAt(i));
            for (int k = 0; k < folded.length(); k++) {
                char fc = folded.charAt(k);
                if (fc == ' ') {
                    if (!lastWasSpace) {
                        normBuilder.append(' ');
                        normToOrigIndex.add(i);
                        lastWasSpace = true;
                    }
                } else {
                    normBuilder.append(fc);
                    normToOrigIndex.add(i);
                    lastWasSpace = false;
                }
            }
        }
        String normalized = normBuilder.toString().trim();
        if (normalized.isEmpty()) {
            return new FilterOutcome(false, false, rawContent, List.of());
        }

        boolean shouldBlock = false;
        Set<String> matchedWords = new LinkedHashSet<>();
        List<int[]> maskRanges = new ArrayList<>();

        for (CompiledRule rule : activeRules) {
            Matcher m = rule.boundaryPattern.matcher(normalized);
            while (m.find()) {
                matchedWords.add(rule.entity.getWord());
                if (rule.entity.getAction() == FilterAction.BLOCK) {
                    shouldBlock = true;
                } else {
                    int origStart = normToOrigIndex.get(m.start());
                    int origEnd = normToOrigIndex.get(m.end() - 1) + 1;
                    maskRanges.add(new int[]{origStart, origEnd});
                }
            }
        }

        if (shouldBlock) {
            recordViolation(userId, conversationId, rawContent, String.join(", ", matchedWords), "BLOCK");
            return new FilterOutcome(true, false, rawContent, new ArrayList<>(matchedWords));
        }

        if (!maskRanges.isEmpty()) {
            String maskedText = applyMaskRanges(rawContent, maskRanges);
            recordViolation(userId, conversationId, rawContent, String.join(", ", matchedWords), "MASK");
            return new FilterOutcome(false, true, maskedText, new ArrayList<>(matchedWords));
        }

        return new FilterOutcome(false, false, rawContent, List.of());
    }

    private String applyMaskRanges(String original, List<int[]> ranges) {
        ranges.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        for (int[] r : ranges) {
            if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < r[0]) {
                merged.add(new int[]{r[0], r[1]});
            } else {
                merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], r[1]);
            }
        }
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (int[] r : merged) {
            out.append(original, cursor, r[0]);
            out.append("***");
            cursor = r[1];
        }
        out.append(original.substring(cursor));
        return out.toString();
    }

    private void recordViolation(Integer userId, Long convId, String raw, String matched, String action) {
        if (userId == null) return;
        ChatViolation v = new ChatViolation();
        v.setUserId(userId);
        v.setConversationId(convId);
        v.setRawContent(raw);
        v.setMatchedWord(matched);
        v.setActionTaken(action);
        violationRepository.save(v);
    }

    public List<BannedWord> listAll() {
        return bannedWordRepository.findAllByOrderByIdDesc();
    }

    public List<ChatViolation> listRecentViolations() {
        return violationRepository.findTop50ByOrderByIdDesc();
    }

    @Transactional
    public BannedWord addOrUpdateWord(String word, FilterAction action, String severity, Integer adminId) {
        String norm = normalizeText(word);
        if (norm.isBlank()) {
            throw new IllegalArgumentException("Từ cấm không hợp lệ sau chuẩn hóa");
        }
        BannedWord bw = bannedWordRepository.findByNormalizedWord(norm).orElseGet(BannedWord::new);
        bw.setWord(word.trim());
        bw.setNormalizedWord(norm);
        bw.setAction(action != null ? action : FilterAction.MASK);
        bw.setSeverity(severity != null ? severity : "MEDIUM");
        bw.setActive(true);
        bw.setCreatedBy(adminId);
        BannedWord saved = bannedWordRepository.save(bw);
        reloadCache();
        return saved;
    }

    @Transactional
    public void deleteWord(Integer wordId) {
        bannedWordRepository.deleteById(wordId);
        reloadCache();
    }
}
