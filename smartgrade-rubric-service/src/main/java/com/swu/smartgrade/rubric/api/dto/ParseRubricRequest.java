package com.swu.smartgrade.rubric.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseRubricRequest(@NotBlank @Size(min = 20, max = 10000) String sourceText) {
}
