package com.swu.smartgrade.user.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.user.api.dto.ProfileResponse;
import com.swu.smartgrade.user.application.CommentProfileApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/teachers")
@Tag(name = "C01 身份与教师偏好服务")
public class InternalTeacherController {
    private final CommentProfileApplicationService profiles;

    public InternalTeacherController(CommentProfileApplicationService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{teacherId}/comment-profile")
    @Operation(operationId = "C01-4-03", summary = "读取指定教师评语偏好")
    public ApiResponse<InternalProfile> profile(@PathVariable Long teacherId) {
        return ApiResponse.ok(InternalProfile.from(profiles.get(teacherId)), TraceId.current());
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
