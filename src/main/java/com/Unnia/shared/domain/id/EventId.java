package com.Unnia.shared.domain.id;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record EventId(UUID value) {

	public EventId {
		Guard.notNull(value, "EventId é obrigatório");
	}

	public static EventId generate() {
		return new EventId(UUID.randomUUID());
	}

	public static EventId of(String raw) {
		try {
			return new EventId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("EventId inválido");
		}
	}
}
