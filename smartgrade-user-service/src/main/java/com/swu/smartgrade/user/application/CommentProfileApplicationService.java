package com.swu.smartgrade.user.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.user.api.dto.ProfileRequest;
import com.swu.smartgrade.user.api.dto.ProfileResponse;
import com.swu.smartgrade.user.domain.CommentProfile;
import com.swu.smartgrade.user.domain.CommentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class CommentProfileApplicationService {
    private final CommentProfileRepository repository;
    private final ObjectMapper objectMapper;

    public CommentProfileApplicationService(CommentProfileRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(Long teacherId) {
        return repository.findByTeacherId(teacherId)
                .map(this::toResponse)
                .orElseGet(() -> new ProfileResponse(null, teacherId, "暂未设置", "NEUTRAL",
                        "MEDIUM", "DIRECT", List.of(), List.of(), 0,
                        java.time.OffsetDateTime.now()));
    }

    public ProfileResponse getFor(AuthPrincipal principal) {
        requireTeacher(principal);
        return get(principal.userId());
    }

    @Transactional
    public ProfileResponse save(Long teacherId, ProfileRequest request) {
        CommentProfile profile = repository.findByTeacherId(teacherId).orElseGet(() -> {
            CommentProfile created = new CommentProfile();
            created.setProfileId(System.currentTimeMillis());
            created.setTeacherId(teacherId);
            created.setProfileVersion(0);
            return created;
        });
        profile.setTone(request.tone());
        profile.setLengthMode(request.lengthMode());
        profile.setOpeningStyle(request.openingStyle());
        profile.setCommonPhrases(write(request.commonPhrases()));
        profile.setBannedPhrases(write(request.bannedPhrases()));
        profile.setProfileVersion(profile.getProfileVersion() + 1);
        profile.setUpdatedAt(LocalDateTime.now());
        return toResponse(repository.save(profile));
    }

    public ProfileResponse saveFor(AuthPrincipal principal, ProfileRequest request) {
        requireTeacher(principal);
        return save(principal.userId(), request);
    }

    private void requireTeacher(AuthPrincipal principal) {
        if (!principal.isTeacher()) {
            throw BusinessException.forbidden("仅教师可以维护评语偏好");
        }
    }

    private ProfileResponse toResponse(CommentProfile profile) {
        return new ProfileResponse(profile.getProfileId(), profile.getTeacherId(),
                profile.getTone() + "·" + profile.getLengthMode(),
                profile.getTone(), profile.getLengthMode(), profile.getOpeningStyle(),
                read(profile.getCommonPhrases()), read(profile.getBannedPhrases()),
                profile.getProfileVersion(),
                profile.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    private String write(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw BusinessException.badRequest("评语偏好无法序列化");
        }
    }

    private List<String> read(String value) {
        try {
            return value == null || value.isBlank()
                    ? List.of()
                    : objectMapper.readValue(value, new TypeReference<>() {
                    });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
