package com.swu.smartgrade.rubric.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePointsRequest(
        // 输入限制：至少 1 个要点，最多 50 个，且每个要点必须通过 RubricPointRequest 校验。
        @NotEmpty @Size(max = 50) List<@Valid RubricPointRequest> points
) {
}
