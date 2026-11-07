package com.Unnia.shared.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Token bucket por chave, em memória. As chaves vêm de dados controlados pelo cliente (IP, conta),
 * então o cache tem tamanho máximo e expira baldes ociosos: um atacante não consegue inflar a memória
 * inventando chaves.
 */
public class InMemoryRateLimiter implements RateLimiter {

	static final long MAX_KEYS = 100_000;
	static final Duration IDLE_EXPIRY = Duration.ofMinutes(10);

	private final Cache<String, Bucket> buckets;
	private final Clock clock;

	public InMemoryRateLimiter(Clock clock) {
		this(clock, false);
	}

	/** {@code synchronousEviction} torna a expiração determinística; usado nos testes. */
	InMemoryRateLimiter(Clock clock, boolean synchronousEviction) {
		this.clock = clock;
		Caffeine<Object, Object> builder = Caffeine.newBuilder().maximumSize(MAX_KEYS)
				.expireAfterAccess(IDLE_EXPIRY).ticker(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis()));
		if (synchronousEviction) {
			builder.executor(Runnable::run);
		}
		this.buckets = builder.build();
	}

	@Override
	public Decision tryAcquire(String key, RateLimit limit) {
		long now = TimeUnit.MILLISECONDS.toNanos(clock.millis());
		return buckets.get(key, k -> new Bucket(limit, now)).tryAcquire(now);
	}

	private static final class Bucket {

		private static final double EPSILON = 1e-9;

		private final double capacity;
		private final double tokensPerNano;
		private double tokens;
		private long lastRefill;

		Bucket(RateLimit limit, long now) {
			this.capacity = limit.capacity();
			this.tokensPerNano = limit.capacity() / (double) limit.window().toNanos();
			this.tokens = capacity;
			this.lastRefill = now;
		}

		synchronized Decision tryAcquire(long now) {
			long elapsed = Math.max(0, now - lastRefill);
			tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
			lastRefill = now;
			// tolerância para o erro de ponto flutuante ao somar frações de token (0,999… vale 1)
			if (tokens + EPSILON >= 1) {
				tokens -= 1;
				return new Decision(true, (int) Math.floor(tokens), Duration.ZERO);
			}
			long waitNanos = (long) Math.ceil((1 - tokens) / tokensPerNano);
			return new Decision(false, 0, Duration.ofNanos(waitNanos));
		}
	}
}
