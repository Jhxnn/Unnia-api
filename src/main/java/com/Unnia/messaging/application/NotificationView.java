package com.Unnia.messaging.application;

import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.messaging.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationView(NotificationId notificationId, NotificationType type, String text, UUID referenceId,
		Instant createdAt, boolean read) {

	public static NotificationView from(Notification notification) {
		return new NotificationView(notification.id(), notification.type(), notification.text(),
				notification.referenceId().orElse(null), notification.createdAt(), notification.isRead());
	}
}
