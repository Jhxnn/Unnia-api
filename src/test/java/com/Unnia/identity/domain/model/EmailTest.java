package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

	@Test
	void normalizesToLowerCaseAndTrimsWhitespace() {
		assertThat(Email.of("  Ong@Exemplo.COM ").value()).isEqualTo("ong@exemplo.com");
	}

	@Test
	void emailsWithSameNormalizedValueAreEqual() {
		assertThat(Email.of("A@b.com")).isEqualTo(Email.of("a@B.com"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   ", "semarroba", "@semlocal.com", "sem@dominio", "dois@@arrobas.com", "com espaco@x.com"})
	void rejectsInvalidEmails(String raw) {
		assertThatThrownBy(() -> Email.of(raw)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsTooLongEmail() {
		assertThatThrownBy(() -> Email.of("a".repeat(250) + "@x.com")).isInstanceOf(DomainException.class);
	}
}
