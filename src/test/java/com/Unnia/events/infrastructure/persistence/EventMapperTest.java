package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.Capacity;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.events.domain.model.EventTitle;
import com.Unnia.events.domain.model.Location;
import com.Unnia.events.domain.model.Period;
import com.Unnia.events.domain.model.RegistrationStatus;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final Instant START = NOW.plusSeconds(7 * 24 * 3600);

	private Event publishedEvent() {
		Event event = Event.create(NgoId.generate(), EventTitle.of("Mutirão"), Description.of("Doação"),
				Location.of("Rua A, 10", City.of("Recife")), Period.of(START, START.plusSeconds(4 * 3600)),
				Capacity.of(3), NOW);
		event.publish(NOW);
		return event;
	}

	@Test
	void newEventIsCopiedFieldByFieldIntoTheEntity() {
		Event event = publishedEvent();
		VolunteerId volunteer = VolunteerId.generate();
		event.register(volunteer, NOW);

		EventEntity entity = new EventEntity();
		EventMapper.apply(event, entity);

		assertThat(entity.id).isEqualTo(event.id().value());
		assertThat(entity.ownerId).isEqualTo(event.ownerId().value());
		assertThat(entity.title).isEqualTo("Mutirão");
		assertThat(entity.city).isEqualTo("Recife");
		assertThat(entity.capacity).isEqualTo(3);
		assertThat(entity.status).isEqualTo(EventStatus.PUBLISHED);
		assertThat(entity.registrations).hasSize(1);
		assertThat(entity.registrations.get(0).volunteerId).isEqualTo(volunteer.value());
		assertThat(entity.version).isNull();
	}

	@Test
	void roundTripRestoresTheAggregateWithItsRegistrations() {
		Event event = publishedEvent();
		VolunteerId volunteer = VolunteerId.generate();
		event.register(volunteer, NOW);
		EventEntity entity = new EventEntity();
		EventMapper.apply(event, entity);

		Event restored = EventMapper.toDomain(entity);

		assertThat(restored).isEqualTo(event);
		assertThat(restored.isRegistered(volunteer)).isTrue();
		assertThat(restored.availableSpots()).isEqualTo(2);
		assertThat(restored.period()).isEqualTo(event.period());
		assertThat(restored.domainEvents()).isEmpty();
	}

	@Test
	void applyingAgainUpdatesRegistrationsInPlaceAndKeepsTheVersion() {
		Event event = publishedEvent();
		VolunteerId first = VolunteerId.generate();
		VolunteerId second = VolunteerId.generate();
		event.register(first, NOW);
		EventEntity entity = new EventEntity();
		EventMapper.apply(event, entity);
		entity.version = 7L;
		RegistrationEntity firstRegistration = entity.registrations.get(0);

		event.register(second, NOW);
		event.cancelRegistration(first, NOW);
		EventMapper.apply(event, entity);

		assertThat(entity.version).isEqualTo(7L);
		assertThat(entity.registrations).hasSize(2);
		assertThat(entity.registrations.get(0)).isSameAs(firstRegistration);
		assertThat(entity.registrations.get(0).status).isEqualTo(RegistrationStatus.CANCELLED);
		assertThat(entity.registrations.get(1).volunteerId).isEqualTo(second.value());
	}
}
