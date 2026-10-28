package com.Unnia.messaging.application.handler;

import com.Unnia.events.domain.event.EventCancelled;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.port.AccountResolver;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

/** Avisa cada voluntário inscrito de que o evento foi cancelado. */
public class EventCancelledHandler implements DomainEventHandler<EventCancelled> {

	private final AccountResolver accounts;
	private final Notifier notifier;

	public EventCancelledHandler(AccountResolver accounts, Notifier notifier) {
		this.accounts = accounts;
		this.notifier = notifier;
	}

	@Override
	public Class<EventCancelled> eventType() {
		return EventCancelled.class;
	}

	@Override
	public void handle(EventCancelled event) {
		event.affectedVolunteers().forEach(volunteerId -> accounts.accountOfVolunteer(volunteerId)
				.ifPresent(recipient -> notifier.notify(recipient, NotificationType.EVENT_CANCELLED,
						"Um evento em que você estava inscrito foi cancelado", event.eventId().value())));
	}
}
