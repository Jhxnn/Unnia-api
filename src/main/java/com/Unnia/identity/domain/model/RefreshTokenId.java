package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.Identifier;

import java.util.UUID;

public record RefreshTokenId(UUID value) implements Identifier {

	public RefreshTokenId {
		Guard.notNull(value, "RefreshTokenId é obrigatório");
	}

	public static RefreshTokenId generate() {
		return new RefreshTokenId(UUID.randomUUID());
	}

	public static RefreshTokenId of(String raw) {
		try {
			return new RefreshTokenId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("RefreshTokenId inválido");
		}
	}
}
