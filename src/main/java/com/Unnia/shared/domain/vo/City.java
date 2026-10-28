package com.Unnia.shared.domain.vo;

import com.Unnia.shared.domain.Guard;

public record City(String value) {

	public City {
		value = Guard.maxLength(Guard.notBlank(value, "Cidade é obrigatória"), 80,
				"Cidade deve ter no máximo 80 caracteres");
	}

	public static City of(String value) {
		return new City(value);
	}
}
