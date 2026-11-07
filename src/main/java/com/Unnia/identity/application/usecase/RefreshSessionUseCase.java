package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.SessionPolicy;
import com.Unnia.identity.application.port.TokenGenerator;
import com.Unnia.identity.application.port.TokenHasher;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.RefreshTokenRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Troca um token de renovação por um novo (rotação). Reapresentar um token já usado revoga a sessão
 * inteira, pois indica que ele vazou.
 * <p>
 * O resultado é um valor, e não uma exceção, de propósito: este caso de uso é transacional e uma
 * exceção desfaria a revogação da família que acabou de ser feita.
 */
public class RefreshSessionUseCase {

	public sealed interface Result permits Refreshed, Rejected {
	}

	public record Refreshed(AccountId accountId, Role role, String refreshToken, Instant refreshExpiresAt)
			implements Result {
	}

	/** Token desconhecido, expirado, revogado, reutilizado ou de conta suspensa. */
	public record Rejected() implements Result {
	}

	private static final Rejected REJECTED = new Rejected();

	private final RefreshTokenRepository refreshTokens;
	private final AccountRepository accounts;
	private final TokenGenerator generator;
	private final TokenHasher hasher;
	private final SessionPolicy policy;
	private final Clock clock;

	public RefreshSessionUseCase(RefreshTokenRepository refreshTokens, AccountRepository accounts,
			TokenGenerator generator, TokenHasher hasher, SessionPolicy policy, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.accounts = accounts;
		this.generator = generator;
		this.hasher = hasher;
		this.policy = policy;
		this.clock = clock;
	}

	public Result execute(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return REJECTED;
		}
		Instant now = clock.instant();
		Optional<RefreshToken> found = refreshTokens.findByTokenHash(hasher.hash(rawToken));
		if (found.isEmpty()) {
			return REJECTED;
		}
		RefreshToken token = found.get();

		if (token.wasUsed()) {
			refreshTokens.revokeFamily(token.familyId(), now);
			return REJECTED;
		}
		if (!token.isUsable(now)) {
			return REJECTED;
		}
		Optional<Account> account = accounts.findById(token.accountId()).filter(Account::isActive);
		if (account.isEmpty()) {
			refreshTokens.revokeFamily(token.familyId(), now);
			return REJECTED;
		}

		String newRaw = generator.newToken();
		RefreshToken next = token.rotate(hasher.hash(newRaw), now, policy.refreshTokenTtl());
		refreshTokens.save(token);
		refreshTokens.save(next);
		return new Refreshed(account.get().id(), account.get().role(), newRaw, next.expiresAt());
	}
}
