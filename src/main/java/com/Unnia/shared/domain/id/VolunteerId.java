package com.Unnia.shared.domain.id;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record VolunteerId(UUID value) {

	public VolunteerId {
		Guard.notNull(value, "VolunteerId é obrigatório");
	}

	public static VolunteerId generate() {
		return new VolunteerId(UUID.randomUUID());
	}

	public static VolunteerId of(String raw) {
		try {
			return new VolunteerId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("VolunteerId inválido");
		}
	}
}
