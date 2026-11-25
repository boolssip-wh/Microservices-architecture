package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.application.ReviewDeliveryCoordinator;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.api.TraceId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/demo/seata")
public class InternalSeataDemoController {
    private final ReviewDeliveryCoordinator coordinator;
    private final boolean remoteControlEnabled;

    public InternalSeataDemoController(
            ReviewDeliveryCoordinator coordinator,
            @Value("${app.seata-demo.remote-control-enabled:true}") boolean remoteControlEnabled) {
        this.coordinator = coordinator;
        this.remoteControlEnabled = remoteControlEnabled;
    }

    @PutMapping("/fail-after-remote/{enabled}")
    public ApiResponse<Map<String, Object>> setFailAfterRemote(@PathVariable boolean enabled) {
        if (!remoteControlEnabled) {
            throw BusinessException.forbidden("当前环境未开放 Seata 演示故障开关");
        }
        coordinator.setFailAfterRemote(enabled);
        return ApiResponse.ok(Map.of("failAfterRemote", coordinator.isFailAfterRemote()), TraceId.current());
    }
}
