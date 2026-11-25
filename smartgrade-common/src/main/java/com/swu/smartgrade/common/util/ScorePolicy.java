package com.swu.smartgrade.common.util;

import com.swu.smartgrade.common.api.BusinessException;
import java.math.BigDecimal;

public final class ScorePolicy {
    private ScorePolicy() { }

    public static BigDecimal baseScore(BigDecimal maximum, BigDecimal requested) {
        if (maximum == null || maximum.signum() <= 0) {
            throw BusinessException.badRequest("满分必须大于0");
        }
        BigDecimal base = requested == null ? BigDecimal.TEN.min(maximum) : requested;
        if (base.signum() < 0 || base.compareTo(maximum) > 0) {
            throw BusinessException.badRequest("基础分必须在0到满分之间");
        }
        if (base.stripTrailingZeros().scale() > 2) {
            throw BusinessException.badRequest("基础分最多保留两位小数");
        }
        return base.setScale(2);
    }
}
