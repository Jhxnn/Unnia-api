package com.Unnia.shared.domain.id;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record AccountId(UUID value) {

	public AccountId {
		Guard.notNull(value, "AccountId é obrigatório");
	}

	public static AccountId generate() {
		return new AccountId(UUID.randomUUID());
	}

	public static AccountId of(String raw) {
		try {
			return new AccountId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("AccountId inválido");
		}
	}
}
