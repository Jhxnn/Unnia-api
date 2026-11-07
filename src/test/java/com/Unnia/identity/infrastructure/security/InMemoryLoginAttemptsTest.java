package com.Unnia.identity.infrastructure.security;

import com.Unnia.shared.application.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptsTest {

	private static final Duration WINDOW = Duration.ofMinutes(15);

	private MutableClock clock;
	private InMemoryLoginAttempts attempts;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
		attempts = new InMemoryLoginAttempts(clock, 5, WINDOW, true);
	}

	private void failTimes(String key, int times) {
		for (int i = 0; i < times; i++) {
			attempts.recordFailure(key);
		}
	}

	@Test
	void staysOpenBelowTheLimit() {
		failTimes("maria", 4);

		assertThat(attempts.blockedFor("maria")).isEmpty();
		assertThat(attempts.blockedFor("desconhecida")).isEmpty();
	}

	@Test
	void blocksAtTheLimitForTheRestOfTheWindow() {
		failTimes("maria", 5);

		assertThat(attempts.blockedFor("maria")).contains(WINDOW);
	}

	@Test
	void theRemainingWaitShrinksAsTimePasses() {
		failTimes("maria", 5);

		clock.advance(Duration.ofMinutes(10));

		assertThat(attempts.blockedFor("maria")).contains(Duration.ofMinutes(5));
	}

	@Test
	void furtherFailuresDoNotExtendTheWindow() {
		failTimes("maria", 5);
		clock.advance(Duration.ofMinutes(10));

		failTimes("maria", 3);

		assertThat(attempts.blockedFor("maria")).contains(Duration.ofMinutes(5));
	}

	@Test
	void unblocksWhenTheWindowEndsAndCountingStartsOver() {
		failTimes("maria", 5);

		clock.advance(WINDOW.plusSeconds(1));
		assertThat(attempts.blockedFor("maria")).isEmpty();

		failTimes("maria", 4);
		assertThat(attempts.blockedFor("maria")).isEmpty();
		attempts.recordFailure("maria");
		assertThat(attempts.blockedFor("maria")).isPresent();
	}

	@Test
	void failuresOutsideTheWindowDoNotAccumulate() {
		failTimes("maria", 4);

		clock.advance(WINDOW.plusSeconds(1));
		failTimes("maria", 4);

		assertThat(attempts.blockedFor("maria")).isEmpty();
	}

	@Test
	void resetClearsTheHistory() {
		failTimes("maria", 5);

		attempts.reset("maria");

		assertThat(attempts.blockedFor("maria")).isEmpty();
		failTimes("maria", 4);
		assertThat(attempts.blockedFor("maria")).isEmpty();
	}

	@Test
	void keysAreIndependent() {
		failTimes("maria", 5);

		assertThat(attempts.blockedFor("maria")).isPresent();
		assertThat(attempts.blockedFor("joao")).isEmpty();
	}
}
