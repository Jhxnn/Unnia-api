package com.Unnia.identity.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecureCodeGeneratorTest {

	private final SecureCodeGenerator generator = new SecureCodeGenerator();

	@Test
	void alwaysHasSixDigitsEvenWithLeadingZeros() {
		for (int i = 0; i < 2000; i++) {
			assertThat(generator.newCode()).matches("\\d{6}");
		}
	}

	@Test
	void doesNotRepeatTheSameCodeOverAndOver() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			seen.add(generator.newCode());
		}

		assertThat(seen.size()).isGreaterThan(150);
	}
}
