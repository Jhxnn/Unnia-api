package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationTokenTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final Duration TTL = Duration.ofMinutes(15);

	private EmailVerificationToken token() {
		return EmailVerificationToken.issue(AccountId.generate(), "hash", NOW, TTL);
	}

	@Test
	void aFreshCodeIsUsableUntilItExpires() {
		EmailVerificationToken token = token();

		assertThat(token.isUsable(NOW)).isTrue();
		assertThat(token.isUsable(NOW.plus(TTL).minusSeconds(1))).isTrue();
		assertThat(token.isUsable(NOW.plus(TTL))).isFalse();
		assertThat(token.failedAttempts()).isZero();
	}

	@Test
	void consumingSpendsTheCode() {
		EmailVerificationToken token = token();

		token.consume(NOW.plusSeconds(5));

		assertThat(token.isUsable(NOW.plusSeconds(6))).isFalse();
		assertThat(token.usedAt()).contains(NOW.plusSeconds(5));
		assertThatThrownBy(() -> token.consume(NOW.plusSeconds(10))).isInstanceOf(DomainException.class)
				.hasMessage(EmailVerificationToken.INVALID_CODE);
	}

	@Test
	void anExpiredCodeCannotBeConsumed() {
		EmailVerificationToken token = token();

		assertThatThrownBy(() -> token.consume(NOW.plus(TTL).plusSeconds(1))).isInstanceOf(DomainException.class);
		assertThat(token.usedAt()).isEmpty();
	}

	@Test
	void wrongAttemptsAreCountedAndTheCodeDiesAtTheLimit() {
		EmailVerificationToken token = token();

		for (int i = 1; i < EmailVerificationToken.MAX_FAILED_ATTEMPTS; i++) {
			token.recordFailure(NOW.plusSeconds(i));
			assertThat(token.isUsable(NOW.plusSeconds(i))).isTrue();
		}
		token.recordFailure(NOW.plusSeconds(10));

		assertThat(token.failedAttempts()).isEqualTo(EmailVerificationToken.MAX_FAILED_ATTEMPTS);
		assertThat(token.isUsable(NOW.plusSeconds(11))).isFalse();
	}

	@Test
	void failuresAfterTheCodeWasSpentAreIgnored() {
		EmailVerificationToken token = token();
		token.consume(NOW);

		token.recordFailure(NOW.plusSeconds(1));

		assertThat(token.failedAttempts()).isZero();
	}

	@Test
	void invalidatingIsIdempotentAndKeepsTheFirstMoment() {
		EmailVerificationToken token = token();

		token.invalidate(NOW.plusSeconds(1));
		token.invalidate(NOW.plusSeconds(9));

		assertThat(token.usedAt()).contains(NOW.plusSeconds(1));
		assertThat(token.isUsable(NOW.plusSeconds(10))).isFalse();
	}

	@Test
	void theSecretTiesTheCodeToTheAccount() {
		AccountId one = AccountId.generate();
		AccountId other = AccountId.generate();

		assertThat(EmailVerificationToken.secretOf(one, "123456")).isNotEqualTo(EmailVerificationToken.secretOf(other, "123456"));
		assertThat(EmailVerificationToken.secretOf(one, "123456")).endsWith(":123456");
	}

	@Test
	void requiresTheMandatoryFields() {
		assertThatThrownBy(() -> EmailVerificationToken.issue(null, "hash", NOW, TTL)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> EmailVerificationToken.issue(AccountId.generate(), " ", NOW, TTL))
				.isInstanceOf(DomainException.class);
	}
}
