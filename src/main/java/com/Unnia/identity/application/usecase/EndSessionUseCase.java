package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.TokenHasher;
import com.Unnia.identity.domain.repository.RefreshTokenRepository;

import java.time.Clock;

/** Logout: revoga a sessão do token informado. Idempotente: token desconhecido não é erro. */
public class EndSessionUseCase {

	private final RefreshTokenRepository refreshTokens;
	private final TokenHasher hasher;
	private final Clock clock;

	public EndSessionUseCase(RefreshTokenRepository refreshTokens, TokenHasher hasher, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.hasher = hasher;
		this.clock = clock;
	}

	public void execute(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		refreshTokens.findByTokenHash(hasher.hash(rawToken))
				.ifPresent(token -> refreshTokens.revokeFamily(token.familyId(), clock.instant()));
	}
}
