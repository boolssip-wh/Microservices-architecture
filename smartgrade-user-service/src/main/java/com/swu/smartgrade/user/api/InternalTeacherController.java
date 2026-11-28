package com.swu.smartgrade.user.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.user.api.dto.ProfileResponse;
import com.swu.smartgrade.user.application.CommentProfileApplicationService;
import com.swu.smartgrade.user.application.NotificationChannelApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * A4/B2 内部接口：供 review-service 读取教师评语偏好和通知接收方式。
 */
@RestController
@RequestMapping("/internal/teachers")
@Tag(name = "C01 身份与教师偏好服务")
public class InternalTeacherController {
    private final CommentProfileApplicationService profiles;
    private final NotificationChannelApplicationService channels;

    public InternalTeacherController(CommentProfileApplicationService profiles,
                                     NotificationChannelApplicationService channels) {
        this.profiles = profiles;
        this.channels = channels;
    }

    @GetMapping("/{teacherId}/comment-profile")
    @Operation(operationId = "C01-4-03", summary = "读取指定教师评语偏好")
    public ApiResponse<InternalProfile> profile(@PathVariable Long teacherId) {
        return ApiResponse.ok(InternalProfile.from(profiles.get(teacherId)), TraceId.current());
    }

    @GetMapping("/{teacherId}/notification-channel")
    @Operation(operationId = "C01-5-03", summary = "供通知服务读取教师短信通道")
    public ApiResponse<NotificationChannelApplicationService.RawChannel> notificationChannel(
            @PathVariable Long teacherId) {
        return ApiResponse.ok(channels.raw(teacherId), TraceId.current());
    }

    public record InternalProfile(Long teacherId, int profileVersion, ProfileResponse profile, Long profileId, String styleTag,
                                  String tone, String lengthMode, String openingStyle,
                                  java.util.List<String> commonPhrases, java.util.List<String> bannedPhrases) {
        static InternalProfile from(ProfileResponse profile) {
            return new InternalProfile(profile.teacherId(), profile.profileVersion(), profile, profile.profileId(), profile.styleTag(),
                    profile.tone(), profile.lengthMode(), profile.openingStyle(),
                    profile.commonPhrases(), profile.bannedPhrases());
        }
    }
}
