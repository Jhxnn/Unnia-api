package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.Identifier;

import java.util.UUID;

public record EmailVerificationTokenId(UUID value) implements Identifier {

	public EmailVerificationTokenId {
		Guard.notNull(value, "EmailVerificationTokenId é obrigatório");
	}

	public static EmailVerificationTokenId generate() {
		return new EmailVerificationTokenId(UUID.randomUUID());
	}

	public static EmailVerificationTokenId of(String raw) {
		try {
			return new EmailVerificationTokenId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("EmailVerificationTokenId inválido");
		}
	}
}
