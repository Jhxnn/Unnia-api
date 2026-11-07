package com.Unnia.identity.application.fakes;

import com.Unnia.identity.application.port.LoginAttempts;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bloqueia a chave depois de {@code maxFailures} falhas, por um tempo fixo. */
public class FakeLoginAttempts implements LoginAttempts {

	public static final Duration BLOCK = Duration.ofMinutes(15);

	private final int maxFailures;
	private final Map<String, Integer> failures = new HashMap<>();

	public FakeLoginAttempts(int maxFailures) {
		this.maxFailures = maxFailures;
	}

	@Override
	public Optional<Duration> blockedFor(String key) {
		return failures.getOrDefault(key, 0) >= maxFailures ? Optional.of(BLOCK) : Optional.empty();
	}

	@Override
	public void recordFailure(String key) {
		failures.merge(key, 1, Integer::sum);
	}

	@Override
	public void reset(String key) {
		failures.remove(key);
	}

	public int failuresOf(String key) {
		return failures.getOrDefault(key, 0);
	}
}
