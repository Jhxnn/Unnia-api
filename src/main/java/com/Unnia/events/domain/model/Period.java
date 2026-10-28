package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.Guard;

import java.time.Instant;

public record Period(Instant start, Instant end) {

	public Period {
		Guard.notNull(start, "Início é obrigatório");
		Guard.notNull(end, "Fim é obrigatório");
		Guard.check(end.isAfter(start), "Fim deve ser posterior ao início");
	}

	public static Period of(Instant start, Instant end) {
		return new Period(start, end);
	}

	public boolean hasStarted(Instant now) {
		return !now.isBefore(start);
	}

	public boolean hasEnded(Instant now) {
		return !now.isBefore(end);
	}
}
