package com.Unnia.messaging.application;

import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;

public record MessageView(MessageId messageId, AccountId senderId, String text, Instant sentAt, Instant readAt) {

	public static MessageView from(Message message) {
		return new MessageView(message.id(), message.senderId(), message.content().text(), message.sentAt(),
				message.readAt().orElse(null));
	}
}
