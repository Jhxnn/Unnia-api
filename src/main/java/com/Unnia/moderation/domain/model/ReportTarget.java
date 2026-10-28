package com.Unnia.moderation.domain.model;

import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record ReportTarget(ReportTargetType type, UUID id) {

	public ReportTarget {
		Guard.notNull(type, "Tipo do alvo é obrigatório");
		Guard.notNull(id, "Alvo é obrigatório");
	}
}
