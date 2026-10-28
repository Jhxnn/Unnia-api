package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

	@Test
	void neverExposesHashInToString() {
		assertThat(PasswordHash.of("segredo-hash").toString()).doesNotContain("segredo-hash");
	}

	@Test
	void rejectsBlank() {
		assertThatThrownBy(() -> PasswordHash.of(" ")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> PasswordHash.of(null)).isInstanceOf(DomainException.class);
	}
}
