package com.swu.smartgrade.report.api.dto;

public record ScoreBand(String key, String label, long count, double percentage) {
}
