package com.Unnia.shared.domain;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SharedKernelTest {

	record SomethingHappened(String what) implements DomainEvent {
	}

	static class Sample extends AggregateRoot<AccountId> {
		Sample(AccountId id) {
			super(id);
		}

		void happen(String what) {
			raise(new SomethingHappened(what));
		}
	}

	@Test
	void aggregateAccumulatesAndReleasesDomainEventsOnce() {
		Sample sample = new Sample(AccountId.generate());
		sample.happen("a");
		sample.happen("b");

		assertThat(sample.domainEvents()).hasSize(2);
		assertThat(sample.pullDomainEvents()).containsExactly(new SomethingHappened("a"),
				new SomethingHappened("b"));
		assertThat(sample.pullDomainEvents()).isEmpty();
	}

	@Test
	void entitiesWithSameIdentityAreEqual() {
		AccountId id = AccountId.generate();

		assertThat(new Sample(id)).isEqualTo(new Sample(id)).hasSameHashCodeAs(new Sample(id));
		assertThat(new Sample(id)).isNotEqualTo(new Sample(AccountId.generate()));
	}

	@Test
	void entityRequiresIdentity() {
		assertThatThrownBy(() -> new Sample(null)).isInstanceOf(DomainException.class);
	}

	@Test
	void identifiersGenerateParseAndValidate() {
		UUID uuid = UUID.randomUUID();

		assertThat(AccountId.of(uuid.toString()).value()).isEqualTo(uuid);
		assertThat(AccountId.generate()).isNotEqualTo(AccountId.generate());
		assertThatThrownBy(() -> AccountId.of("nao-e-uuid")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> AccountId.of(null)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> new AccountId(null)).isInstanceOf(DomainException.class);
	}

	@Test
	void displayNameTrimsAndLimitsLength() {
		assertThat(DisplayName.of("  Maria  ").value()).isEqualTo("Maria");
		assertThatThrownBy(() -> DisplayName.of("x".repeat(121))).isInstanceOf(DomainException.class);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	void displayNameAndCityRejectBlank(String raw) {
		assertThatThrownBy(() -> DisplayName.of(raw)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> City.of(raw)).isInstanceOf(DomainException.class);
	}

	@Test
	void cityLimitsLength() {
		assertThat(City.of(" Recife ").value()).isEqualTo("Recife");
		assertThatThrownBy(() -> City.of("x".repeat(81))).isInstanceOf(DomainException.class);
	}

	@Test
	void descriptionAllowsEmptyButLimitsLength() {
		assertThat(Description.of(null)).isEqualTo(Description.EMPTY);
		assertThat(Description.of("  texto ").value()).isEqualTo("texto");
		assertThatThrownBy(() -> Description.of("x".repeat(2001))).isInstanceOf(DomainException.class);
	}
}
