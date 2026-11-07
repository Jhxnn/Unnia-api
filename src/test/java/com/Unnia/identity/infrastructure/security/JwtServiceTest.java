package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

	private static final String SECRET = "a-secret-with-clearly-more-than-32-chars";
	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	private MutableClock clock;
	private JwtService service;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(NOW);
		service = new JwtService(SECRET, Duration.ofHours(2), clock);
	}

	@Test
	void issuedTokenRoundTripsAccountAndRole() {
		AccountId account = AccountId.generate();

		JwtService.IssuedToken token = service.issue(account, Role.NGO);

		assertThat(service.parse(token.value())).contains(new AuthenticatedUser(account, Role.NGO));
		assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(2)));
	}

	@Test
	void expiredTokenIsRejected() {
		JwtService.IssuedToken token = service.issue(AccountId.generate(), Role.VOLUNTEER);

		clock.advance(Duration.ofHours(2).plusSeconds(1));

		assertThat(service.parse(token.value())).isEmpty();
	}

	@Test
	void tokenIsStillValidJustBeforeExpiry() {
		JwtService.IssuedToken token = service.issue(AccountId.generate(), Role.VOLUNTEER);

		clock.advance(Duration.ofHours(2).minusSeconds(1));

		assertThat(service.parse(token.value())).isPresent();
	}

	@Test
	void tamperedPayloadIsRejected() {
		String token = service.issue(AccountId.generate(), Role.VOLUNTEER).value();
		String[] parts = token.split("\\.");
		String forged = parts[0] + "." + flipFirstChar(parts[1]) + "." + parts[2];

		assertThat(service.parse(forged)).isEmpty();
	}

	@Test
	void tokenSignedWithAnotherKeyIsRejected() {
		JwtService other = new JwtService("another-secret-that-is-also-longer-than-32", Duration.ofHours(2), clock);
		String foreign = other.issue(AccountId.generate(), Role.ADMIN).value();

		assertThat(service.parse(foreign)).isEmpty();
	}

	@Test
	void garbageIsRejectedWithoutThrowing() {
		assertThat(service.parse("")).isEqualTo(Optional.empty());
		assertThat(service.parse("not-a-jwt")).isEmpty();
		assertThat(service.parse("a.b.c")).isEmpty();
	}

	@Test
	void refusesToStartWithAMissingOrShortSecret() {
		assertThatThrownBy(() -> new JwtService("", Duration.ofHours(1), clock))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtService("short-secret", Duration.ofHours(1), clock))
				.isInstanceOf(IllegalStateException.class);
	}

	private String flipFirstChar(String segment) {
		char first = segment.charAt(0);
		return (first == 'A' ? 'B' : 'A') + segment.substring(1);
	}
}
