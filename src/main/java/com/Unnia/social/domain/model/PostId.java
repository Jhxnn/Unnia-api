package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record PostId(UUID value) {

	public PostId {
		Guard.notNull(value, "PostId é obrigatório");
	}

	public static PostId generate() {
		return new PostId(UUID.randomUUID());
	}

	public static PostId of(String raw) {
		try {
			return new PostId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("PostId inválido");
		}
	}
}
