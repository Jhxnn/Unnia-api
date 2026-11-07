package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final Duration TTL = Duration.ofDays(30);
	private static final AccountId ACCOUNT = AccountId.generate();

	private RefreshToken token() {
		return RefreshToken.startSession(ACCOUNT, "hash-1", NOW, TTL);
	}

	@Test
	void startsASessionInANewFamilyUsableUntilItExpires() {
		RefreshToken token = token();

		assertThat(token.accountId()).isEqualTo(ACCOUNT);
		assertThat(token.tokenHash()).isEqualTo("hash-1");
		assertThat(token.expiresAt()).isEqualTo(NOW.plus(TTL));
		assertThat(token.usedAt()).isEmpty();
		assertThat(token.wasUsed()).isFalse();
		assertThat(token.isUsable(NOW)).isTrue();
		assertThat(token.isUsable(NOW.plus(TTL).minusSeconds(1))).isTrue();
		assertThat(token.isUsable(NOW.plus(TTL))).isFalse();
		assertThat(token().familyId()).isNotEqualTo(token.familyId());
	}

	@Test
	void rotatingConsumesTheTokenAndKeepsTheFamily() {
		RefreshToken first = token();
		Instant later = NOW.plusSeconds(60);

		RefreshToken second = first.rotate("hash-2", later, TTL);

		assertThat(first.wasUsed()).isTrue();
		assertThat(first.usedAt()).contains(later);
		assertThat(first.isUsable(later)).isFalse();
		assertThat(second.familyId()).isEqualTo(first.familyId());
		assertThat(second.accountId()).isEqualTo(ACCOUNT);
		assertThat(second.tokenHash()).isEqualTo("hash-2");
		assertThat(second.expiresAt()).isEqualTo(later.plus(TTL));
		assertThat(second.isUsable(later)).isTrue();
		assertThat(second).isNotEqualTo(first);
	}

	@Test
	void aTokenCanOnlyBeRotatedOnce() {
		RefreshToken token = token();
		token.rotate("hash-2", NOW, TTL);

		assertThatThrownBy(() -> token.rotate("hash-3", NOW, TTL)).isInstanceOf(DomainException.class);
	}

	@Test
	void revokedAndExpiredTokensCannotBeRotated() {
		RefreshToken revoked = token();
		revoked.revoke(NOW);
		RefreshToken expired = token();

		assertThatThrownBy(() -> revoked.rotate("h", NOW, TTL)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> expired.rotate("h", NOW.plus(TTL), TTL)).isInstanceOf(DomainException.class);
	}

	@Test
	void revokingIsIdempotentAndKeepsTheFirstInstant() {
		RefreshToken token = token();

		token.revoke(NOW.plusSeconds(10));
		token.revoke(NOW.plusSeconds(99));

		assertThat(token.revokedAt()).contains(NOW.plusSeconds(10));
		assertThat(token.isUsable(NOW)).isFalse();
		assertThat(token.wasUsed()).isFalse();
	}

	@Test
	void requiresAccountAndHash() {
		assertThatThrownBy(() -> RefreshToken.startSession(null, "h", NOW, TTL)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> RefreshToken.startSession(ACCOUNT, " ", NOW, TTL))
				.isInstanceOf(DomainException.class);
	}
}
