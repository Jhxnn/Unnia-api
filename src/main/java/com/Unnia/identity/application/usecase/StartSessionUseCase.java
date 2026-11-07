package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.SessionPolicy;
import com.Unnia.identity.application.port.TokenGenerator;
import com.Unnia.identity.application.port.TokenHasher;
import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.identity.domain.repository.RefreshTokenRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;
import java.time.Instant;

/** Abre uma sessão após o login: emite o primeiro token de renovação de uma nova família. */
public class StartSessionUseCase {

	public record Result(String refreshToken, Instant expiresAt) {
	}

	private final RefreshTokenRepository refreshTokens;
	private final TokenGenerator generator;
	private final TokenHasher hasher;
	private final SessionPolicy policy;
	private final Clock clock;

	public StartSessionUseCase(RefreshTokenRepository refreshTokens, TokenGenerator generator, TokenHasher hasher,
			SessionPolicy policy, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.generator = generator;
		this.hasher = hasher;
		this.policy = policy;
		this.clock = clock;
	}

	public Result execute(AccountId accountId) {
		String raw = generator.newToken();
		RefreshToken token = RefreshToken.startSession(accountId, hasher.hash(raw), clock.instant(),
				policy.refreshTokenTtl());

		refreshTokens.save(token);
		return new Result(raw, token.expiresAt());
	}
}
