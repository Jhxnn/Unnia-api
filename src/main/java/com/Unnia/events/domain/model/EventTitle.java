package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.Guard;

public record EventTitle(String value) {

	public EventTitle {
		value = Guard.maxLength(Guard.notBlank(value, "Título é obrigatório"), 150,
				"Título deve ter no máximo 150 caracteres");
	}

	public static EventTitle of(String value) {
		return new EventTitle(value);
	}
}
