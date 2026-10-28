package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.Guard;

/** Senha em texto puro. Existe só em trânsito, até virar {@link PasswordHash}. */
public record RawPassword(String value) {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 72;

	public RawPassword {
		Guard.notNull(value, "Senha é obrigatória");
		Guard.check(value.length() >= MIN_LENGTH, "A senha deve ter no mínimo " + MIN_LENGTH + " caracteres");
		Guard.check(value.length() <= MAX_LENGTH, "A senha deve ter no máximo " + MAX_LENGTH + " caracteres");
		Guard.check(value.chars().anyMatch(Character::isLetter) && value.chars().anyMatch(Character::isDigit),
				"A senha deve conter letras e números");
	}

	public static RawPassword of(String value) {
		return new RawPassword(value);
	}

	@Override
	public String toString() {
		return "RawPassword[****]";
	}
}
