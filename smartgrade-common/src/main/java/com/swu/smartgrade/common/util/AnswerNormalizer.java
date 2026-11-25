package com.swu.smartgrade.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class AnswerNormalizer {
    private AnswerNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    public static Set<String> optionSet(String value) {
        return normalize(value).chars()
                .mapToObj(character -> String.valueOf((char) character))
                .filter(item -> item.matches("[a-z0-9]"))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static String judge(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "对", "正确", "√", "true", "t", "1" -> "正确";
            case "错", "错误", "×", "x", "false", "f", "0" -> "错误";
            default -> normalized;
        };
    }
}
