package com.swu.smartgrade.grading.api.dto;

public record OcrRequest(String answerId, String objectKey, String text) {
}
