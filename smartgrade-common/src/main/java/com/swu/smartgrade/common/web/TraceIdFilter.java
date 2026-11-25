package com.swu.smartgrade.common.web;

import com.swu.smartgrade.common.api.TraceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceId.ensure(request.getHeader(TraceId.HEADER));
        response.setHeader(TraceId.HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceId.clear();
        }
    }
}
