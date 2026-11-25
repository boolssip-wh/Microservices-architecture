package com.swu.smartgrade.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

@Configuration
public class SentinelGatewayConfiguration {
    @PostConstruct
    public void registerBlockHandler() {
        BlockRequestHandler handler = (exchange, throwable) -> ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "code", 42900,
                        "message", "请求过于集中，Sentinel 网关限流已生效",
                        "path", exchange.getRequest().getPath().value(),
                        "timestamp", OffsetDateTime.now().toString()
                ));
        GatewayCallbackManager.setBlockHandler(handler);
    }
}
