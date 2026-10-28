package com.Unnia.shared.domain.id;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record NgoId(UUID value) {

	public NgoId {
		Guard.notNull(value, "NgoId é obrigatório");
	}

	public static NgoId generate() {
		return new NgoId(UUID.randomUUID());
	}

	public static NgoId of(String raw) {
		try {
			return new NgoId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("NgoId inválido");
		}
	}
}
