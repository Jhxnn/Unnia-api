package com.Unnia.shared.infrastructure.ratelimit;

import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.infrastructure.ratelimit.RateLimiter.Decision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRateLimiterTest {

	private static final RateLimit TEN_PER_MINUTE = RateLimit.perMinute(10);

	private MutableClock clock;
	private InMemoryRateLimiter limiter;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
		limiter = new InMemoryRateLimiter(clock, true);
	}

	private void consume(String key, int times) {
		for (int i = 0; i < times; i++) {
			limiter.tryAcquire(key, TEN_PER_MINUTE);
		}
	}

	@Test
	void allowsUpToTheCapacityThenRejects() {
		for (int expectedRemaining = 9; expectedRemaining >= 0; expectedRemaining--) {
			Decision decision = limiter.tryAcquire("k", TEN_PER_MINUTE);

			assertThat(decision.allowed()).isTrue();
			assertThat(decision.remaining()).isEqualTo(expectedRemaining);
			assertThat(decision.retryAfter()).isZero();
		}

		Decision rejected = limiter.tryAcquire("k", TEN_PER_MINUTE);

		assertThat(rejected.allowed()).isFalse();
		assertThat(rejected.remaining()).isZero();
	}

	@Test
	void tellsHowLongToWaitForTheNextRequest() {
		consume("k", 10);

		Decision rejected = limiter.tryAcquire("k", TEN_PER_MINUTE);

		// 10 por minuto = um token a cada 6 segundos
		assertThat(rejected.retryAfter()).isEqualTo(Duration.ofSeconds(6));
	}

	@Test
	void refillsContinuouslyWithTime() {
		consume("k", 10);

		clock.advance(Duration.ofSeconds(5));
		assertThat(limiter.tryAcquire("k", TEN_PER_MINUTE).allowed()).isFalse();

		clock.advance(Duration.ofSeconds(1));
		assertThat(limiter.tryAcquire("k", TEN_PER_MINUTE).allowed()).isTrue();
		assertThat(limiter.tryAcquire("k", TEN_PER_MINUTE).allowed()).isFalse();
	}

	@Test
	void neverAccumulatesMoreThanTheCapacityWhileIdle() {
		consume("k", 3);

		clock.advance(Duration.ofMinutes(5));
		for (int i = 0; i < 10; i++) {
			assertThat(limiter.tryAcquire("k", TEN_PER_MINUTE).allowed()).isTrue();
		}

		assertThat(limiter.tryAcquire("k", TEN_PER_MINUTE).allowed()).isFalse();
	}

	@Test
	void keysAreIndependent() {
		consume("a", 10);

		assertThat(limiter.tryAcquire("a", TEN_PER_MINUTE).allowed()).isFalse();
		assertThat(limiter.tryAcquire("b", TEN_PER_MINUTE).allowed()).isTrue();
	}

	@Test
	void idleBucketsExpireAndStartFullAgain() {
		consume("k", 10);

		clock.advance(InMemoryRateLimiter.IDLE_EXPIRY.plusSeconds(1));

		Decision decision = limiter.tryAcquire("k", TEN_PER_MINUTE);
		assertThat(decision.allowed()).isTrue();
		assertThat(decision.remaining()).isEqualTo(9);
	}

	@Test
	void limitsWithOtherWindowsRefillAtTheirOwnRate() {
		RateLimit twoPerTenSeconds = new RateLimit(2, Duration.ofSeconds(10));
		limiter.tryAcquire("k", twoPerTenSeconds);
		limiter.tryAcquire("k", twoPerTenSeconds);
		assertThat(limiter.tryAcquire("k", twoPerTenSeconds).retryAfter()).isEqualTo(Duration.ofSeconds(5));

		clock.advance(Duration.ofSeconds(5));

		assertThat(limiter.tryAcquire("k", twoPerTenSeconds).allowed()).isTrue();
	}

	@Test
	void rateLimitValidatesItsParameters() {
		assertThatThrownBy(() -> new RateLimit(0, Duration.ofMinutes(1))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> new RateLimit(5, Duration.ZERO)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> new RateLimit(5, null)).isInstanceOf(DomainException.class);
	}
}
