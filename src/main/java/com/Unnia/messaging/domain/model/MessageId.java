package com.Unnia.messaging.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record MessageId(UUID value) {

	public MessageId {
		Guard.notNull(value, "MessageId é obrigatório");
	}

	public static MessageId generate() {
		return new MessageId(UUID.randomUUID());
	}

	public static MessageId of(String raw) {
		try {
			return new MessageId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("MessageId inválido");
		}
	}
}
