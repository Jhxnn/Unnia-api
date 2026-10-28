package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.domain.id.AccountId;

public class CountUnreadNotificationsUseCase {

	private final NotificationRepository notifications;

	public CountUnreadNotificationsUseCase(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	public long execute(AccountId accountId) {
		return notifications.countUnreadFor(accountId);
	}
}
