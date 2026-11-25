package com.swu.smartgrade.rubric.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.rubric.domain.RubricRepository;
import com.swu.smartgrade.rubric.infrastructure.BatchAccessClient;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RubricAccessService {
    private final RubricRepository rubrics;
    private final BatchAccessClient batches;
    private final String serviceToken;

    public RubricAccessService(RubricRepository rubrics, BatchAccessClient batches,
                              @Value("${SERVICE_TOKEN:local-service-token}") String serviceToken) {
        this.rubrics = rubrics;
        this.batches = batches;
        this.serviceToken = serviceToken;
    }

    public String requireRubric(String rubricId) {
        String batchId = rubrics.findById(rubricId)
                .orElseThrow(() -> BusinessException.notFound("评分规则不存在")).getBatchId();
        requireBatch(batchId);
        return rubricId;
    }

    public String requireBatch(String batchId) {
        ownedBatch(batchId);
        return batchId;
    }

    private java.util.Map<String, Object> ownedBatch(String batchId) {
        AuthPrincipal principal = AuthPrincipal.required();
        try {
            var response = batches.detail(batchId, principal.userId().toString(),
                    TraceId.current(), serviceToken);
            if (response == null || response.code() != 0 || response.data() == null) {
                throw BusinessException.forbidden("无权访问该批次评分规则");
            }
            return response.data();
        } catch (FeignException.NotFound ex) {
            throw BusinessException.notFound("阅卷批次不存在");
        } catch (FeignException.Forbidden ex) {
            throw BusinessException.forbidden("无权访问该批次评分规则");
        }
    }

    public com.swu.smartgrade.rubric.api.dto.CreateRubricRequest requireCreate(
            com.swu.smartgrade.rubric.api.dto.CreateRubricRequest request) {
        var batch = ownedBatch(request.batchId());
        Object maximum = batch.get("maxScore");
        if (maximum != null && new java.math.BigDecimal(maximum.toString()).compareTo(request.maxScore()) != 0) {
            throw BusinessException.badRequest("评分规则满分必须与批次满分一致");
        }
        Object baseValue = batch.get("baseScore");
        java.math.BigDecimal batchBase = com.swu.smartgrade.common.util.ScorePolicy.baseScore(request.maxScore(),
                baseValue == null ? null : new java.math.BigDecimal(baseValue.toString()));
        if (request.baseScore() != null && batchBase.compareTo(request.baseScore()) != 0) {
            throw BusinessException.badRequest("评分规则基础分必须与批次基础分一致");
        }
        return new com.swu.smartgrade.rubric.api.dto.CreateRubricRequest(request.batchId(), request.sourceText(),
                request.maxScore(), batchBase);
    }
}
