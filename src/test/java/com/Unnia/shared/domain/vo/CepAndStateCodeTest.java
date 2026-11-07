package com.Unnia.shared.domain.vo;

import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CepAndStateCodeTest {

	@Test
	void cepAcceptsDigitsAndTheUsualMasks() {
		assertThat(Cep.of("50030230").value()).isEqualTo("50030230");
		assertThat(Cep.of("50030-230").value()).isEqualTo("50030230");
		assertThat(Cep.of(" 50.030-230 ").value()).isEqualTo("50030230");
	}

	@Test
	void cepIsFormattedWithTheHyphen() {
		assertThat(Cep.of("50030230").formatted()).isEqualTo("50030-230");
	}

	@Test
	void cepRejectsWrongLengthAndLetters() {
		assertThatThrownBy(() -> Cep.of("1234567")).isInstanceOf(DomainException.class).hasMessage("CEP deve ter 8 dígitos");
		assertThatThrownBy(() -> Cep.of("123456789")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Cep.of("5003023a")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Cep.of("")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Cep.of(null)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> new Cep("50030-230")).isInstanceOf(DomainException.class);
	}

	@Test
	void optionalCepTurnsBlankIntoNullButStillValidatesWhenFilled() {
		assertThat(Cep.ofNullable(null)).isNull();
		assertThat(Cep.ofNullable("  ")).isNull();
		assertThat(Cep.ofNullable("50030-230")).isEqualTo(Cep.of("50030230"));
		assertThatThrownBy(() -> Cep.ofNullable("123")).isInstanceOf(DomainException.class);
	}

	@Test
	void stateCodeNormalizesCaseAndSpaces() {
		assertThat(StateCode.of("pe").value()).isEqualTo("PE");
		assertThat(StateCode.of(" sp ").value()).isEqualTo("SP");
	}

	@Test
	void stateCodeCoversAllTwentySevenUnits() {
		String[] all = { "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR",
				"PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO" };
		assertThat(all).hasSize(27);
		for (String uf : all) {
			assertThat(StateCode.of(uf).value()).isEqualTo(uf);
		}
	}

	@Test
	void stateCodeRejectsUnknownSiglasAndFullNames() {
		assertThatThrownBy(() -> StateCode.of("XX")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> StateCode.of("Pernambuco")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> StateCode.of("P")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> StateCode.of(null)).isInstanceOf(DomainException.class);
	}

	@Test
	void optionalStateTurnsBlankIntoNullButStillValidatesWhenFilled() {
		assertThat(StateCode.ofNullable(null)).isNull();
		assertThat(StateCode.ofNullable("")).isNull();
		assertThat(StateCode.ofNullable("pe")).isEqualTo(StateCode.of("PE"));
		assertThatThrownBy(() -> StateCode.ofNullable("ZZ")).isInstanceOf(DomainException.class);
	}
}
