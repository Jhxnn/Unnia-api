package com.Unnia.messaging.application.handler;

import com.Unnia.events.domain.event.RegistrationCancelled;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.port.AccountResolver;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

/** Avisa a ONG organizadora que um voluntário cancelou a inscrição. */
public class RegistrationCancelledHandler implements DomainEventHandler<RegistrationCancelled> {

	private final AccountResolver accounts;
	private final Notifier notifier;

	public RegistrationCancelledHandler(AccountResolver accounts, Notifier notifier) {
		this.accounts = accounts;
		this.notifier = notifier;
	}

	@Override
	public Class<RegistrationCancelled> eventType() {
		return RegistrationCancelled.class;
	}

	@Override
	public void handle(RegistrationCancelled event) {
		accounts.accountOfNgo(event.ownerId()).ifPresent(recipient -> notifier.notify(recipient,
				NotificationType.REGISTRATION_CANCELLED, "Um voluntário cancelou a inscrição no seu evento",
				event.eventId().value()));
	}
}
