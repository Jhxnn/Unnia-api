package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.Guard;

public record PasswordHash(String value) {

	public PasswordHash {
		value = Guard.notBlank(value, "Senha é obrigatória");
	}

	public static PasswordHash of(String value) {
		return new PasswordHash(value);
	}

	@Override
	public String toString() {
		return "PasswordHash[****]";
	}
}
