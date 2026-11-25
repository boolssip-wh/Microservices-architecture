package com.swu.smartgrade.review.api.dto;

public record CommentView(String suggestedText, String finalText, String status, Integer profileVersion) {
}
