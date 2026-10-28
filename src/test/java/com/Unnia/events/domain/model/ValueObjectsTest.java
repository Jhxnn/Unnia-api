package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.vo.City;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

	private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");

	@Test
	void periodRequiresEndAfterStart() {
		assertThatThrownBy(() -> Period.of(T0, T0)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Period.of(T0, T0.minusSeconds(1))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Period.of(null, T0)).isInstanceOf(DomainException.class);
	}

	@Test
	void periodKnowsWhenItStartedAndEnded() {
		Period period = Period.of(T0, T0.plusSeconds(3600));

		assertThat(period.hasStarted(T0.minusSeconds(1))).isFalse();
		assertThat(period.hasStarted(T0)).isTrue();
		assertThat(period.hasEnded(T0.plusSeconds(3599))).isFalse();
		assertThat(period.hasEnded(T0.plusSeconds(3600))).isTrue();
	}

	@Test
	void capacityNeedsAtLeastOneSpot() {
		assertThat(Capacity.of(1).max()).isEqualTo(1);
		assertThatThrownBy(() -> Capacity.of(0)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Capacity.of(-5)).isInstanceOf(DomainException.class);
	}

	@Test
	void titleIsRequiredAndLimited() {
		assertThat(EventTitle.of(" Mutirão ").value()).isEqualTo("Mutirão");
		assertThatThrownBy(() -> EventTitle.of(" ")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> EventTitle.of("x".repeat(151))).isInstanceOf(DomainException.class);
	}

	@Test
	void locationRequiresAddressAndCity() {
		assertThat(Location.of(" Rua A, 10 ", City.of("Recife")).address()).isEqualTo("Rua A, 10");
		assertThatThrownBy(() -> Location.of("", City.of("Recife"))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Location.of("Rua A", null)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Location.of("x".repeat(201), City.of("Recife")))
				.isInstanceOf(DomainException.class);
	}
}
