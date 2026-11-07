package com.Unnia.shared.infrastructure.ratelimit;

import com.Unnia.shared.domain.Guard;

import java.time.Duration;

/** Até {@code capacity} requisições por {@code window}, repostas de forma contínua (token bucket). */
public record RateLimit(int capacity, Duration window) {

	public RateLimit {
		Guard.check(capacity >= 1, "A capacidade do limite deve ser de pelo menos 1");
		Guard.notNull(window, "A janela do limite é obrigatória");
		Guard.check(!window.isZero() && !window.isNegative(), "A janela do limite deve ser positiva");
	}

	public static RateLimit perMinute(int capacity) {
		return new RateLimit(capacity, Duration.ofMinutes(1));
	}
}
