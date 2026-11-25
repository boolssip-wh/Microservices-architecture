package com.swu.smartgrade.common.api;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceId {
    public static final String HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    private TraceId() {
    }

    public static String current() {
        String value = MDC.get(MDC_KEY);
        return value == null ? "-" : value;
    }

    public static String ensure(String incoming) {
        String value = incoming == null || incoming.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : incoming;
        MDC.put(MDC_KEY, value);
        return value;
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
