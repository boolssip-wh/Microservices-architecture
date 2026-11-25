package com.swu.smartgrade.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CommonWebConfig implements WebMvcConfigurer {
    private final AuthHeaderInterceptor authHeaderInterceptor;

    public CommonWebConfig(AuthHeaderInterceptor authHeaderInterceptor) {
        this.authHeaderInterceptor = authHeaderInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authHeaderInterceptor).addPathPatterns("/api/**", "/internal/**");
    }
}
