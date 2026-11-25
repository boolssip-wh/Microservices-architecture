package com.swu.smartgrade.user.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.user.api.dto.NotificationChannelRequest;
import com.swu.smartgrade.user.api.dto.NotificationChannelView;
import com.swu.smartgrade.user.domain.TeacherNotificationChannel;
import com.swu.smartgrade.user.domain.TeacherNotificationChannelRepository;
import com.swu.smartgrade.user.domain.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class NotificationChannelApplicationService {
    private final TeacherNotificationChannelRepository channels;
    private final TeacherRepository teachers;

    public NotificationChannelApplicationService(TeacherNotificationChannelRepository channels,
                                                 TeacherRepository teachers) {
        this.channels = channels;
        this.teachers = teachers;
    }

    @Transactional(readOnly = true)
    public NotificationChannelView getFor(AuthPrincipal principal) {
        requireTeacher(principal);
        return view(principal.userId());
    }

    @Transactional
    public NotificationChannelView saveFor(AuthPrincipal principal, NotificationChannelRequest request) {
        requireTeacher(principal);
        if (!teachers.existsById(principal.userId())) {
            throw BusinessException.notFound("教师账号不存在");
        }
        TeacherNotificationChannel channel = channels.findById(principal.userId())
                .orElseGet(() -> {
                    TeacherNotificationChannel created = new TeacherNotificationChannel();
                    created.setTeacherId(principal.userId());
                    return created;
                });
        channel.setPhone(normalize(request.phone()));
        channel.setSmsEnabled(Boolean.TRUE.equals(request.enabled()));
        channel.setUpdatedAt(LocalDateTime.now());
        channels.save(channel);
        return view(principal.userId());
    }

    @Transactional(readOnly = true)
    public NotificationChannelView view(Long teacherId) {
        return channels.findById(teacherId)
                .map(channel -> new NotificationChannelView(
                        teacherId,
                        true,
                        mask(channel.getPhone()),
                        channel.isSmsEnabled(),
                        channel.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()))
                .orElseGet(() -> new NotificationChannelView(teacherId, false, null, false, null));
    }

    @Transactional(readOnly = true)
    public RawChannel raw(Long teacherId) {
        return channels.findById(teacherId)
                .map(channel -> new RawChannel(true, channel.isSmsEnabled(), channel.getPhone()))
                .orElseGet(() -> new RawChannel(false, false, null));
    }

    private String normalize(String phone) {
        String value = phone == null ? "" : phone.replaceAll("\\s+", "");
        if (value.startsWith("+86")) {
            value = value.substring(3);
        } else if (value.startsWith("86") && value.length() == 13) {
            value = value.substring(2);
        }
        if (!value.matches("1[3-9]\\d{9}")) {
            throw BusinessException.badRequest("请输入有效的中国大陆手机号");
        }
        return value;
    }

    private String mask(String phone) {
        if (phone == null || phone.length() != 11) return null;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private void requireTeacher(AuthPrincipal principal) {
        if (!principal.isTeacher()) {
            throw BusinessException.forbidden("仅教师可以配置短信通知");
        }
    }

    public record RawChannel(boolean bound, boolean enabled, String phone) {
    }
}
