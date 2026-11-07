package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.application.port.LoginAttempts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Conta falhas de login por chave dentro de uma janela fixa que começa na primeira falha. Ao atingir
 * o máximo a chave fica bloqueada até a janela terminar. O cache é limitado em tamanho e a entrada
 * expira sozinha ao fim da janela, então chaves inventadas não crescem sem limite.
 */
@Component
class InMemoryLoginAttempts implements LoginAttempts {

	private static final long MAX_KEYS = 100_000;

	private record Failures(int count, long windowEndsAt) {
	}

	private final Cache<String, Failures> cache;
	private final int maxFailures;
	private final long windowNanos;
	private final Clock clock;

	@Autowired
	InMemoryLoginAttempts(Clock clock, @Value("${unnia.rate-limit.login-max-failures:5}") int maxFailures,
			@Value("${unnia.rate-limit.login-window:PT15M}") Duration window) {
		this(clock, maxFailures, window, false);
	}

	/** {@code synchronousEviction} torna a expiração determinística; usado nos testes. */
	InMemoryLoginAttempts(Clock clock, int maxFailures, Duration window, boolean synchronousEviction) {
		this.clock = clock;
		this.maxFailures = maxFailures;
		this.windowNanos = window.toNanos();
		this.cache = Caffeine.newBuilder().maximumSize(MAX_KEYS).ticker(this::nowNanos)
				.executor(synchronousEviction ? Runnable::run : ForkJoinPool.commonPool())
				.expireAfter(new Expiry<String, Failures>() {
					@Override
					public long expireAfterCreate(String key, Failures value, long currentTime) {
						return remaining(value, currentTime);
					}

					@Override
					public long expireAfterUpdate(String key, Failures value, long currentTime, long currentDuration) {
						return remaining(value, currentTime);
					}

					@Override
					public long expireAfterRead(String key, Failures value, long currentTime, long currentDuration) {
						return currentDuration;
					}
				}).build();
	}

	@Override
	public Optional<Duration> blockedFor(String key) {
		Failures failures = cache.getIfPresent(key);
		if (failures == null || failures.count() < maxFailures) {
			return Optional.empty();
		}
		long remaining = failures.windowEndsAt() - nowNanos();
		return remaining > 0 ? Optional.of(Duration.ofNanos(remaining)) : Optional.empty();
	}

	@Override
	public void recordFailure(String key) {
		long now = nowNanos();
		cache.asMap().compute(key, (k, current) -> current == null || current.windowEndsAt() <= now
				? new Failures(1, now + windowNanos)
				: new Failures(current.count() + 1, current.windowEndsAt()));
	}

	@Override
	public void reset(String key) {
		cache.invalidate(key);
	}

	private long nowNanos() {
		return TimeUnit.MILLISECONDS.toNanos(clock.millis());
	}

	private static long remaining(Failures value, long currentTime) {
		return Math.max(0, value.windowEndsAt() - currentTime);
	}
}
