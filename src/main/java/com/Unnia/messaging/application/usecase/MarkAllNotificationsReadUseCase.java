package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;
import java.time.Instant;

public class MarkAllNotificationsReadUseCase {

	private final NotificationRepository notifications;
	private final Clock clock;

	public MarkAllNotificationsReadUseCase(NotificationRepository notifications, Clock clock) {
		this.notifications = notifications;
		this.clock = clock;
	}

	public void execute(AccountId accountId) {
		Instant now = clock.instant();
		for (Notification notification : notifications.findUnread(accountId)) {
			notification.markRead(accountId, now);
			notifications.save(notification);
		}
	}
}
