package com.Unnia.shared.domain.vo;

import com.Unnia.shared.domain.Guard;

public record Description(String value) {

	public static final Description EMPTY = new Description("");

	public Description {
		value = value == null ? "" : value.trim();
		Guard.maxLength(value, 2000, "Descrição deve ter no máximo 2000 caracteres");
	}

	public static Description of(String value) {
		return new Description(value);
	}
}
