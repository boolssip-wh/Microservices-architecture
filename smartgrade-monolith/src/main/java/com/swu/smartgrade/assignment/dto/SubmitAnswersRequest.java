package com.swu.smartgrade.assignment.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitAnswersRequest(@NotEmpty(message = "作答内容不能为空") List<Answer> answers) {

    public record Answer(Long questionId, String answer) {
    }
}
