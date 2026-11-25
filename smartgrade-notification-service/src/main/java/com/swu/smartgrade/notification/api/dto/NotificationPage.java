package com.swu.smartgrade.notification.api.dto;

import java.util.List;

public record NotificationPage(List<NotificationView> items, long unreadCount) {
}
