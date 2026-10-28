package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CnpjTest {

	@Test
	void acceptsFormattedCnpjAndKeepsOnlyDigits() {
		assertThat(Cnpj.of("11.222.333/0001-81").value()).isEqualTo("11222333000181");
	}

	@Test
	void formatsCnpjForDisplay() {
		assertThat(Cnpj.of("11222333000181").formatted()).isEqualTo("11.222.333/0001-81");
	}

	@Test
	void sameDigitsAreEqualRegardlessOfFormatting() {
		assertThat(Cnpj.of("11.222.333/0001-81")).isEqualTo(Cnpj.of("11222333000181"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"123", "11222333000182", "00000000000000", "11.222.333/0001-8a"})
	void rejectsInvalidCnpj(String raw) {
		assertThatThrownBy(() -> Cnpj.of(raw)).isInstanceOf(DomainException.class);
	}
}
