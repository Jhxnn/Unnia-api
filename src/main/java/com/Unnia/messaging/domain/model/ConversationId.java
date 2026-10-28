package com.Unnia.messaging.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record ConversationId(UUID value) {

	public ConversationId {
		Guard.notNull(value, "ConversationId é obrigatório");
	}

	public static ConversationId generate() {
		return new ConversationId(UUID.randomUUID());
	}

	public static ConversationId of(String raw) {
		try {
			return new ConversationId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("ConversationId inválido");
		}
	}
}
