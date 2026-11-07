package com.Unnia.shared.domain.vo;

import com.Unnia.shared.domain.DomainException;

/** CEP brasileiro: oito dígitos. Aceita "50030-230" ou "50030230" e guarda só os dígitos. */
public record Cep(String value) {

	public Cep {
		if (value == null || !value.matches("\\d{8}")) {
			throw new DomainException("CEP deve ter 8 dígitos");
		}
	}

	public static Cep of(String raw) {
		if (raw == null) {
			throw new DomainException("CEP deve ter 8 dígitos");
		}
		return new Cep(raw.replaceAll("[\\s.-]", ""));
	}

	/** Para campos opcionais: vazio vira nulo; preenchido precisa ser válido. */
	public static Cep ofNullable(String raw) {
		return raw == null || raw.isBlank() ? null : of(raw);
	}

	/** "50030-230" */
	public String formatted() {
		return value.substring(0, 5) + "-" + value.substring(5);
	}
}
