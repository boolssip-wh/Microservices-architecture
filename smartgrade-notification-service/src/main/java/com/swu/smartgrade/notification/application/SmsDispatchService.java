package com.swu.smartgrade.notification.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.event.DomainEvent;
import com.swu.smartgrade.notification.api.dto.SmsDeliveryView;
import com.swu.smartgrade.notification.domain.SmsDelivery;
import com.swu.smartgrade.notification.domain.SmsDeliveryRepository;
import com.swu.smartgrade.notification.infrastructure.client.NotificationChannelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.internet.MimeMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class SmsDispatchService {
    private static final Logger log = LoggerFactory.getLogger(SmsDispatchService.class);
    private static final Set<String> SMS_EVENTS =
            Set.of("grading.completed", "grading.failed", "review.completed");

    private final NotificationChannelClient channelClient;
    private final SmsDeliveryRepository deliveries;
    private final String serviceToken;
    private final String provider;
    private final String webhookUrl;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final String smtpFrom;

    public SmsDispatchService(NotificationChannelClient channelClient,
                              SmsDeliveryRepository deliveries,
                              @Value("${SERVICE_TOKEN:local-service-token}") String serviceToken,
                              @Value("${app.sms.provider:mock}") String provider,
                              @Value("${app.sms.webhook-url:}") String webhookUrl,
                              @Value("${app.sms.smtp-host:smtp.qq.com}") String smtpHost,
                              @Value("${app.sms.smtp-port:465}") int smtpPort,
                              @Value("${app.sms.smtp-username:}") String smtpUsername,
                              @Value("${app.sms.smtp-password:}") String smtpPassword,
                              @Value("${app.sms.smtp-from:}") String smtpFrom) {
        this.channelClient = channelClient;
        this.deliveries = deliveries;
        this.serviceToken = serviceToken;
        this.provider = provider == null ? "mock" : provider.trim().toLowerCase();
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.smtpHost = smtpHost == null ? "smtp.qq.com" : smtpHost.trim();
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername == null ? "" : smtpUsername.trim();
        this.smtpPassword = smtpPassword == null ? "" : smtpPassword.trim();
        this.smtpFrom = smtpFrom == null ? "" : smtpFrom.trim();
    }

    public void dispatch(DomainEvent event) {
        try {
            if (!SMS_EVENTS.contains(event.type())) return;
            if (event.eventId() != null && deliveries.existsByTeacherIdAndEventId(
                    event.teacherId(), event.eventId())) return;
            NotificationChannelClient.Channel channel = channel(event.teacherId());
            if (channel == null || !channel.bound() || !channel.enabled()) return;
            String message = compact(event.title() + "：" + event.content());
            sendAndRecord(event.teacherId(), event.eventId(), event.type(),
                    channel.contact(), message);
        } catch (Exception ex) {
            log.warn("短信提醒未发送，不影响站内通知: {}", ex.getMessage());
        }
    }

    @Transactional
    public SmsDeliveryView test(Long teacherId) {
        NotificationChannelClient.Channel channel = channel(teacherId);
        if (channel == null || !channel.bound()) {
            throw BusinessException.badRequest("请先绑定手机号");
        }
        if (!channel.enabled()) {
            throw BusinessException.badRequest("请先开启短信通知");
        }
        SmsDelivery delivery = sendAndRecord(teacherId, null, "sms.test",
                channel.contact(), "同标阅卷测试提醒：通知通道已连接，可在本页面查看发送记录。");
        return view(delivery);
    }

    @Transactional(readOnly = true)
    public List<SmsDeliveryView> recent(Long teacherId) {
        return deliveries.findTop20ByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream().map(this::view).toList();
    }

    private SmsDelivery sendAndRecord(Long teacherId, String eventId, String eventType,
                                      String contact, String message) {
        SmsDelivery delivery = new SmsDelivery();
        delivery.setDeliveryId("SMS" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16).toUpperCase());
        delivery.setTeacherId(teacherId);
        delivery.setEventId(eventId);
        delivery.setEventType(eventType);
        delivery.setTargetMasked(mask(contact));
        delivery.setMessage(message);
        delivery.setProvider(provider);
        delivery.setCreatedAt(LocalDateTime.now());
        try {
            if ("webhook".equals(provider)) {
                postWebhook(contact, message);
                delivery.setStatus("SENT");
            } else if ("smtp".equals(provider)) {
                postEmail(contact, message);
                delivery.setStatus("SENT");
            } else {
                delivery.setStatus("MOCKED");
            }
        } catch (Exception ex) {
            delivery.setStatus("FAILED");
            delivery.setErrorMessage(compact(ex.getMessage(), 240));
        }
        return deliveries.save(delivery);
    }

    private void postWebhook(String contact, String message) throws Exception {
        if (webhookUrl.isBlank()) {
            throw new IllegalStateException("SMS_WEBHOOK_URL 未配置");
        }
        String payload = "{\"to\":\"" + json(contact) + "\",\"message\":\"" + json(message) + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<Void> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build()
                .send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("短信网关返回 HTTP " + response.statusCode());
        }
    }

    private void postEmail(String contact, String message) throws Exception {
        if (smtpUsername.isBlank() || smtpPassword.isBlank() || smtpFrom.isBlank()) {
            throw new IllegalStateException("邮箱通知未配置，请设置 SMTP 用户名、授权码和发件地址");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.setPort(smtpPort);
        sender.setUsername(smtpUsername);
        sender.setPassword(smtpPassword);
        sender.setProtocol("smtp");
        var properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", smtpPort == 587 ? "true" : "false");
        properties.put("mail.smtp.ssl.enable", smtpPort == 465 ? "true" : "false");
        properties.put("mail.debug", "false");
        MimeMessage mimeMessage = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
        helper.setFrom(smtpFrom);
        helper.setTo(contact);
        helper.setSubject("同标阅卷任务提醒");
        helper.setText(message, false);
        sender.send(mimeMessage);
    }

    private NotificationChannelClient.Channel channel(Long teacherId) {
        var response = channelClient.get(teacherId, serviceToken);
        return response == null ? null : response.data();
    }

    private SmsDeliveryView view(SmsDelivery delivery) {
        return new SmsDeliveryView(delivery.getDeliveryId(), delivery.getEventType(),
                delivery.getTargetMasked(), delivery.getMessage(), delivery.getProvider(),
                delivery.getStatus(), delivery.getErrorMessage(),
                delivery.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    private String mask(String contact) {
        if (contact == null || contact.isBlank()) return "***********";
        if (contact.contains("@")) {
            int at = contact.indexOf('@');
            String local = contact.substring(0, at);
            String shown = local.length() <= 2 ? local : local.substring(0, 2);
            return shown + "***" + contact.substring(at);
        }
        if (contact.length() != 11) return "***********";
        return contact.substring(0, 3) + "****" + contact.substring(7);
    }

    private String compact(String value) {
        return compact(value, 300);
    }

    private String compact(String value, int limit) {
        String text = Optional.ofNullable(value).orElse("").replaceAll("\\s+", " ").trim();
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    private String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
