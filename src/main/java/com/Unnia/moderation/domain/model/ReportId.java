package com.Unnia.moderation.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record ReportId(UUID value) {

	public ReportId {
		Guard.notNull(value, "ReportId é obrigatório");
	}

	public static ReportId generate() {
		return new ReportId(UUID.randomUUID());
	}

	public static ReportId of(String raw) {
		try {
			return new ReportId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("ReportId inválido");
		}
	}
}
