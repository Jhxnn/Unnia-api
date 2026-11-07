package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenMapperTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final Duration TTL = Duration.ofDays(30);

	@Test
	void roundTripKeepsHashFamilyAndUsage() {
		AccountId account = AccountId.generate();
		RefreshToken first = RefreshToken.startSession(account, "a".repeat(64), NOW, TTL);
		RefreshToken second = first.rotate("b".repeat(64), NOW.plusSeconds(60), TTL);

		RefreshTokenEntity entity = new RefreshTokenEntity();
		RefreshTokenMapper.apply(first, entity);
		RefreshToken restored = RefreshTokenMapper.toDomain(entity);

		assertThat(entity.tokenHash).isEqualTo("a".repeat(64));
		assertThat(entity.usedAt).isEqualTo(NOW.plusSeconds(60));
		assertThat(restored).isEqualTo(first);
		assertThat(restored.familyId()).isEqualTo(second.familyId());
		assertThat(restored.wasUsed()).isTrue();
		assertThat(restored.revokedAt()).isEmpty();
		assertThat(restored.domainEvents()).isEmpty();
	}

	@Test
	void applyingAgainKeepsTheVersionForOptimisticLocking() {
		RefreshToken token = RefreshToken.startSession(AccountId.generate(), "c".repeat(64), NOW, TTL);
		RefreshTokenEntity entity = new RefreshTokenEntity();
		RefreshTokenMapper.apply(token, entity);
		entity.version = 3L;

		token.revoke(NOW.plusSeconds(5));
		RefreshTokenMapper.apply(token, entity);

		assertThat(entity.version).isEqualTo(3L);
		assertThat(entity.revokedAt).isEqualTo(NOW.plusSeconds(5));
	}
}
