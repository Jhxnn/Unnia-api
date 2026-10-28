package com.Unnia.messaging.application.handler;

import com.Unnia.identity.domain.event.NgoRejected;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

public class NgoRejectedHandler implements DomainEventHandler<NgoRejected> {

	private final Notifier notifier;

	public NgoRejectedHandler(Notifier notifier) {
		this.notifier = notifier;
	}

	@Override
	public Class<NgoRejected> eventType() {
		return NgoRejected.class;
	}

	@Override
	public void handle(NgoRejected event) {
		notifier.notify(event.accountId(), NotificationType.NGO_REJECTED,
				"A verificação da sua ONG não foi aprovada", event.ngoId().value());
	}
}
