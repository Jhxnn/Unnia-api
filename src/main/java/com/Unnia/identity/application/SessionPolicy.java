package com.Unnia.identity.application;

import com.Unnia.shared.domain.Guard;

import java.time.Duration;

/** Política de sessão: por quanto tempo um token de renovação permanece válido. */
public record SessionPolicy(Duration refreshTokenTtl) {

	public SessionPolicy {
		Guard.notNull(refreshTokenTtl, "Duração do token de renovação é obrigatória");
		Guard.check(!refreshTokenTtl.isZero() && !refreshTokenTtl.isNegative(),
				"Duração do token de renovação deve ser positiva");
	}
}
