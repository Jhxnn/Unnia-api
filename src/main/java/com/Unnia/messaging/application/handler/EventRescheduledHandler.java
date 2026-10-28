package com.Unnia.messaging.application.handler;

import com.Unnia.events.domain.event.EventRescheduled;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.port.AccountResolver;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

/** Avisa cada voluntário inscrito de que o evento mudou de data. */
public class EventRescheduledHandler implements DomainEventHandler<EventRescheduled> {

	private final AccountResolver accounts;
	private final Notifier notifier;

	public EventRescheduledHandler(AccountResolver accounts, Notifier notifier) {
		this.accounts = accounts;
		this.notifier = notifier;
	}

	@Override
	public Class<EventRescheduled> eventType() {
		return EventRescheduled.class;
	}

	@Override
	public void handle(EventRescheduled event) {
		event.affectedVolunteers().forEach(volunteerId -> accounts.accountOfVolunteer(volunteerId)
				.ifPresent(recipient -> notifier.notify(recipient, NotificationType.EVENT_RESCHEDULED,
						"Um evento em que você está inscrito mudou de data", event.eventId().value())));
	}
}
