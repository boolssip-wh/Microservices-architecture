package com.swu.smartgrade.review.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "smartgrade-user-service")
public interface UserClient {
    @GetMapping("/internal/teachers/{teacherId}/comment-profile")
    ApiResponse<CommentProfile> profile(
            @org.springframework.web.bind.annotation.PathVariable("teacherId") Long teacherId,
            @RequestHeader("X-Username") String username,
            @RequestHeader("X-Real-Name") String realName,
            @RequestHeader("X-Role") String role
    );

    record CommentProfile(
            Long profileId,
            Long teacherId,
            String styleTag,
            String tone,
            String lengthMode,
            String openingStyle,
            List<String> commonPhrases,
            List<String> bannedPhrases,
            Integer profileVersion
    ) {
    }
}
