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
        channel.setContact(normalizeContact(request.contact()));
        if (channel.getPhone() == null) {
            channel.setPhone(channel.getContact());
        }
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
                        mask(channel.getContact() == null ? channel.getPhone() : channel.getContact()),
                        channel.isSmsEnabled(),
                        channel.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime()))
                .orElseGet(() -> new NotificationChannelView(teacherId, false, null, false, null));
    }

    @Transactional(readOnly = true)
    public RawChannel raw(Long teacherId) {
        return channels.findById(teacherId)
                .map(channel -> new RawChannel(true, channel.isSmsEnabled(),
                        channel.getContact() == null ? channel.getPhone() : channel.getContact()))
                .orElseGet(() -> new RawChannel(false, false, null));
    }

    private String normalizeContact(String value) {
        String contact = value == null ? "" : value.replaceAll("\\s+", "");
        if (contact.contains("@")) {
            if (!contact.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw BusinessException.badRequest("请输入有效的邮箱地址");
            }
            return contact.toLowerCase();
        }
        if (contact.startsWith("+86")) {
            contact = contact.substring(3);
        } else if (contact.startsWith("86") && contact.length() == 13) {
            contact = contact.substring(2);
        }
        if (!contact.matches("1[3-9]\\d{9}")) {
            throw BusinessException.badRequest("请输入有效的手机号或邮箱");
        }
        return contact;
    }

    private String mask(String contact) {
        if (contact == null || contact.isBlank()) return null;
        if (contact.contains("@")) {
            int at = contact.indexOf('@');
            String local = contact.substring(0, at);
            String shown = local.length() <= 2 ? local : local.substring(0, 2);
            return shown + "***" + contact.substring(at);
        }
        if (contact.length() != 11) return null;
        return contact.substring(0, 3) + "****" + contact.substring(7);
    }

    private void requireTeacher(AuthPrincipal principal) {
        if (!principal.isTeacher()) {
            throw BusinessException.forbidden("仅教师可以配置短信通知");
        }
    }

    public record RawChannel(boolean bound, boolean enabled, String contact) {
    }
}
