package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record RegistrationId(UUID value) {

	public RegistrationId {
		Guard.notNull(value, "RegistrationId é obrigatório");
	}

	public static RegistrationId generate() {
		return new RegistrationId(UUID.randomUUID());
	}

	public static RegistrationId of(String raw) {
		try {
			return new RegistrationId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("RegistrationId inválido");
		}
	}
}
