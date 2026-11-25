package com.swu.smartgrade.notification.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.notification.api.dto.CreateNotificationRequest;
import com.swu.smartgrade.notification.api.dto.NotificationPage;
import com.swu.smartgrade.notification.api.dto.NotificationView;
import com.swu.smartgrade.notification.api.dto.ReadResponse;
import com.swu.smartgrade.notification.domain.Notification;
import com.swu.smartgrade.notification.domain.NotificationRepository;
import com.swu.smartgrade.common.event.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationApplicationService {
    private final NotificationRepository repository;
    private final SmsDispatchService smsDispatcher;
    private record Subscription(Long teacherId, SseEmitter emitter) {}
    private final CopyOnWriteArrayList<Subscription> emitters = new CopyOnWriteArrayList<>();

    public NotificationApplicationService(NotificationRepository repository,
                                          SmsDispatchService smsDispatcher) {
        this.repository = repository;
        this.smsDispatcher = smsDispatcher;
    }

    @Transactional
    public NotificationView create(CreateNotificationRequest request) {
        return persist(request, null);
    }

    @Transactional
    public NotificationView consume(DomainEvent event) {
        if (event.eventId() == null || event.eventId().isBlank() || event.eventId().length() > 64
                || event.teacherId() == null) {
            throw BusinessException.badRequest("事件编号和教师编号不能为空");
        }
        var existing = repository.findByEventId(event.eventId());
        if (existing.isPresent()) return view(existing.get());
        NotificationView result = persist(new CreateNotificationRequest(event.teacherId(), event.type(),
                event.title(), event.content(), event.batchId()), event.eventId());
        smsDispatcher.dispatch(event);
        return result;
    }

    private NotificationView persist(CreateNotificationRequest request, String eventId) {
        Notification notification = new Notification();
        notification.setNotificationId(id());
        notification.setTeacherId(request.teacherId());
        notification.setType(request.type());
        notification.setTitle(request.title());
        notification.setContent(request.content());
        notification.setBatchId(request.batchId());
        notification.setEventId(eventId);
        notification.setCreatedAt(LocalDateTime.now());
        notification = repository.save(notification);
        NotificationView view = view(notification);
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() {
                            broadcast(request.teacherId(), view);
                        }
                    });
        } else {
            broadcast(request.teacherId(), view);
        }
        return view;
    }

    @Transactional(readOnly = true)
    public NotificationPage list(Long teacherId, boolean unreadOnly, int page, int size) {
        List<Notification> source = unreadOnly
                ? repository.findByTeacherIdAndReadAtIsNullOrderByCreatedAtDesc(teacherId)
                : repository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        long unread = repository.findByTeacherIdAndReadAtIsNullOrderByCreatedAtDesc(teacherId).size();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        int from = Math.min(source.size(), (safePage - 1) * safeSize);
        int to = Math.min(source.size(), from + safeSize);
        return new NotificationPage(source.subList(from, to).stream().map(this::view).toList(), unread);
    }

    @Transactional
    public ReadResponse markRead(Long teacherId, String notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("通知不存在"));
        if (!teacherId.equals(notification.getTeacherId())) {
            throw BusinessException.forbidden("不能修改其他教师的通知");
        }
        notification.setReadAt(LocalDateTime.now());
        repository.save(notification);
        return new ReadResponse(notificationId,
                notification.getReadAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    public SseEmitter stream(Long teacherId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Subscription subscription = new Subscription(teacherId, emitter);
        emitters.add(subscription);
        emitter.onCompletion(() -> emitters.remove(subscription));
        emitter.onTimeout(() -> emitters.remove(subscription));
        emitter.onError(error -> emitters.remove(subscription));
        try {
            emitter.send(SseEmitter.event().name("connected").data("Tongbiao grading notification stream ready"));
        } catch (Exception ex) {
            emitters.remove(subscription);
        }
        return emitter;
    }

    private void broadcast(Long teacherId, NotificationView view) {
        for (Subscription subscription : emitters) {
            if (!teacherId.equals(subscription.teacherId())) continue;
            try {
                subscription.emitter().send(SseEmitter.event().id(view.notificationId()).name(view.type()).data(view));
            } catch (Exception ex) {
                emitters.remove(subscription);
            }
        }
    }

    private NotificationView view(Notification notification) {
        return new NotificationView(notification.getNotificationId(), notification.getType(),
                notification.getTitle(), notification.getContent(), notification.getBatchId(),
                notification.getReadAt() == null ? null
                        : notification.getReadAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                notification.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    private String id() {
        return "N" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
