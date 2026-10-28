package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;

public class MarkNotificationReadUseCase {

	public record Command(AccountId accountId, NotificationId notificationId) {
	}

	private final NotificationRepository notifications;
	private final Clock clock;

	public MarkNotificationReadUseCase(NotificationRepository notifications, Clock clock) {
		this.notifications = notifications;
		this.clock = clock;
	}

	public void execute(Command command) {
		Notification notification = notifications.findById(command.notificationId())
				.orElseThrow(() -> new NotFoundException("Notificação não encontrada"));
		if (!notification.recipientId().equals(command.accountId())) {
			throw new ForbiddenException("A notificação pertence a outra conta");
		}

		notification.markRead(command.accountId(), clock.instant());

		notifications.save(notification);
	}
}
