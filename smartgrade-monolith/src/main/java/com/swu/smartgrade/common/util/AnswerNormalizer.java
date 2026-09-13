package com.swu.smartgrade.common.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 答案归一化：全角转半角、去空白、统一大小写、统一判断题的多种写法。
 * 客观题判分的第一道关卡，避免「格式差异」被误判成错题。
 */
public final class AnswerNormalizer {

    private static final Set<String> TRUE_WORDS = Set.of("正确", "对", "是", "√", "T", "TRUE", "Y", "YES", "A");
    private static final Set<String> FALSE_WORDS = Set.of("错误", "错", "否", "×", "X", "F", "FALSE", "N", "NO", "B");

    private AnswerNormalizer() {
    }

    /**
     * 通用归一化：全角转半角 + 去掉所有空白 + 去首尾标点 + 转大写（英文）。
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = toHalfWidth(raw).trim();
        text = text.replaceAll("\\s+", "");
        text = text.replaceAll("[。，,；;、\"'`]+$", "");
        return text.toUpperCase();
    }

    /**
     * 判断题归一化：把「正确/对/√/T/A」统一成 TRUE，把「错误/错/×/F/B」统一成 FALSE。
     */
    public static String normalizeJudge(String raw) {
        String text = normalize(raw);
        if (TRUE_WORDS.contains(text)) {
            return "TRUE";
        }
        if (FALSE_WORDS.contains(text)) {
            return "FALSE";
        }
        return text;
    }

    /**
     * 选择题选项归一化：拆成无序集合，用于多选题（顺序不影响判分）。
     */
    public static Set<String> normalizeOptionSet(String raw) {
        String text = normalize(raw).replaceAll("[^A-Z]", "");
        return Arrays.stream(text.split(""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static String toHalfWidth(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char ch : input.toCharArray()) {
            if (ch == 0x3000) {
                sb.append(' ');
            } else if (ch >= 0xFF01 && ch <= 0xFF5E) {
                sb.append((char) (ch - 0xFEE0));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
