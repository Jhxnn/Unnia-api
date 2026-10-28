package com.Unnia.shared.domain.vo;

import com.Unnia.shared.domain.Guard;

public record DisplayName(String value) {

	public DisplayName {
		value = Guard.maxLength(Guard.notBlank(value, "Nome é obrigatório"), 120,
				"Nome deve ter no máximo 120 caracteres");
	}

	public static DisplayName of(String value) {
		return new DisplayName(value);
	}
}
