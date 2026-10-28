package com.Unnia.messaging.application.handler;

import com.Unnia.events.domain.event.VolunteerRegistered;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.port.AccountResolver;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

/** Avisa a ONG organizadora que um voluntário se inscreveu no evento. */
public class VolunteerRegisteredHandler implements DomainEventHandler<VolunteerRegistered> {

	private final AccountResolver accounts;
	private final Notifier notifier;

	public VolunteerRegisteredHandler(AccountResolver accounts, Notifier notifier) {
		this.accounts = accounts;
		this.notifier = notifier;
	}

	@Override
	public Class<VolunteerRegistered> eventType() {
		return VolunteerRegistered.class;
	}

	@Override
	public void handle(VolunteerRegistered event) {
		accounts.accountOfNgo(event.ownerId()).ifPresent(recipient -> notifier.notify(recipient,
				NotificationType.VOLUNTEER_REGISTERED, "Um voluntário se inscreveu no seu evento",
				event.eventId().value()));
	}
}
