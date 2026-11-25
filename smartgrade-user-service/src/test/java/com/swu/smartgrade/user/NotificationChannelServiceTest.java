package com.swu.smartgrade.user;

import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.user.api.dto.NotificationChannelRequest;
import com.swu.smartgrade.user.application.NotificationChannelApplicationService;
import com.swu.smartgrade.user.domain.TeacherNotificationChannel;
import com.swu.smartgrade.user.domain.TeacherNotificationChannelRepository;
import com.swu.smartgrade.user.domain.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationChannelServiceTest {
    @Test
    void bindsChinaMobileNumberAndOnlyReturnsMaskedValue() {
        TeacherNotificationChannelRepository channels = mock(TeacherNotificationChannelRepository.class);
        TeacherRepository teachers = mock(TeacherRepository.class);
        TeacherNotificationChannel channel = new TeacherNotificationChannel();
        channel.setTeacherId(1L);
        channel.setPhone("13800000000");
        channel.setSmsEnabled(false);
        channel.setUpdatedAt(java.time.LocalDateTime.now());
        when(teachers.existsById(1L)).thenReturn(true);
        when(channels.findById(1L)).thenReturn(Optional.of(channel));
        when(channels.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new NotificationChannelApplicationService(channels, teachers);

        var view = service.saveFor(
                new AuthPrincipal(1L, "teacher1", "李超", "TEACHER"),
                new NotificationChannelRequest("+8613800138000", true));

        assertThat(view.bound()).isTrue();
        assertThat(view.enabled()).isTrue();
        assertThat(view.maskedPhone()).isEqualTo("138****8000");
        ArgumentCaptor<TeacherNotificationChannel> saved =
                ArgumentCaptor.forClass(TeacherNotificationChannel.class);
        verify(channels).save(saved.capture());
        assertThat(saved.getValue().getContact()).isEqualTo("13800138000");
    }

    @Test
    void rejectsInvalidPhoneWithoutPersistingIt() {
        TeacherRepository teachers = mock(TeacherRepository.class);
        when(teachers.existsById(1L)).thenReturn(true);
        var service = new NotificationChannelApplicationService(
                mock(TeacherNotificationChannelRepository.class), teachers);
        assertThatThrownBy(() -> service.saveFor(
                new AuthPrincipal(1L, "teacher1", "李超", "TEACHER"),
                new NotificationChannelRequest("12345", true)))
                .hasMessageContaining("手机号");
    }
}
