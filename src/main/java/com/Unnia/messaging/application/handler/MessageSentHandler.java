package com.Unnia.messaging.application.handler;

import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.domain.event.MessageSent;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;

public class MessageSentHandler implements DomainEventHandler<MessageSent> {

	private final Notifier notifier;

	public MessageSentHandler(Notifier notifier) {
		this.notifier = notifier;
	}

	@Override
	public Class<MessageSent> eventType() {
		return MessageSent.class;
	}

	@Override
	public void handle(MessageSent event) {
		notifier.notify(event.recipientId(), NotificationType.NEW_MESSAGE, "Você recebeu uma nova mensagem",
				event.conversationId().value());
	}
}
