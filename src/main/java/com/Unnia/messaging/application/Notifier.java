package com.Unnia.messaging.application;

import com.Unnia.messaging.application.port.RealtimeUpdates;
import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;
import java.util.UUID;

/** Cria e persiste notificações; usado pelos handlers de eventos. */
public class Notifier {

	private final NotificationRepository notifications;
	private final Clock clock;
	private final RealtimeUpdates realtime;

	public Notifier(NotificationRepository notifications, Clock clock) {
		this(notifications, clock, RealtimeUpdates.NONE);
	}

	public Notifier(NotificationRepository notifications, Clock clock, RealtimeUpdates realtime) {
		this.notifications = notifications;
		this.clock = clock;
		this.realtime = realtime;
	}

	public void notify(AccountId recipient, NotificationType type, String text, UUID referenceId) {
		notifications.save(Notification.create(recipient, type, text, referenceId, clock.instant()));
		realtime.notifyChange(recipient, type, referenceId);
	}
}
