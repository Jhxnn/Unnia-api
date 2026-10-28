package com.Unnia.messaging.application.handler;

import com.Unnia.identity.domain.event.NgoVerified;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

public class NgoVerifiedHandler implements DomainEventHandler<NgoVerified> {

	private final Notifier notifier;

	public NgoVerifiedHandler(Notifier notifier) {
		this.notifier = notifier;
	}

	@Override
	public Class<NgoVerified> eventType() {
		return NgoVerified.class;
	}

	@Override
	public void handle(NgoVerified event) {
		notifier.notify(event.accountId(), NotificationType.NGO_VERIFIED,
				"Sua ONG foi verificada e já pode publicar eventos", event.ngoId().value());
	}
}
