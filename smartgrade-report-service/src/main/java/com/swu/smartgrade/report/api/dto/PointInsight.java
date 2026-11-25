package com.swu.smartgrade.report.api.dto;

public record PointInsight(String pointId, String pointName, long missCount, double missRate) {
}
