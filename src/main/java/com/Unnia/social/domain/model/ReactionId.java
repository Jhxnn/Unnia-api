package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record ReactionId(UUID value) {

	public ReactionId {
		Guard.notNull(value, "ReactionId é obrigatório");
	}

	public static ReactionId generate() {
		return new ReactionId(UUID.randomUUID());
	}

	public static ReactionId of(String raw) {
		try {
			return new ReactionId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("ReactionId inválido");
		}
	}
}
