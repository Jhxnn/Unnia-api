package com.Unnia.shared.domain.vo;

import com.Unnia.shared.domain.DomainException;

import java.util.Locale;
import java.util.Set;

/** Sigla de um estado brasileiro (ou do Distrito Federal), como "PE". */
public record StateCode(String value) {

	private static final Set<String> VALID = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
			"MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

	public StateCode {
		if (value == null || !VALID.contains(value)) {
			throw new DomainException("Estado inválido: use a sigla, por exemplo PE");
		}
	}

	public static StateCode of(String raw) {
		if (raw == null) {
			throw new DomainException("Estado inválido: use a sigla, por exemplo PE");
		}
		return new StateCode(raw.trim().toUpperCase(Locale.ROOT));
	}

	/** Para campos opcionais: vazio vira nulo; preenchido precisa ser válido. */
	public static StateCode ofNullable(String raw) {
		return raw == null || raw.isBlank() ? null : of(raw);
	}
}
