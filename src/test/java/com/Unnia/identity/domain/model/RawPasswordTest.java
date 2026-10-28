package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

	@Test
	void acceptsPasswordWithLettersAndDigits() {
		assertThat(RawPassword.of("senha1234").value()).isEqualTo("senha1234");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"curta1", "somenteletras", "123456789"})
	void rejectsWeakPasswords(String raw) {
		assertThatThrownBy(() -> RawPassword.of(raw)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsTooLongPassword() {
		assertThatThrownBy(() -> RawPassword.of("a1".repeat(37))).isInstanceOf(DomainException.class);
	}

	@Test
	void neverExposesValueInToString() {
		assertThat(RawPassword.of("senha1234").toString()).doesNotContain("senha1234");
	}
}
