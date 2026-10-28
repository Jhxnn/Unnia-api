package com.Unnia.events.domain.model;

import com.Unnia.events.domain.event.AttendanceRecorded;
import com.Unnia.events.domain.event.EventCancelled;
import com.Unnia.events.domain.event.EventCreated;
import com.Unnia.events.domain.event.EventFinished;
import com.Unnia.events.domain.event.EventPublished;
import com.Unnia.events.domain.event.EventRescheduled;
import com.Unnia.events.domain.event.RegistrationCancelled;
import com.Unnia.events.domain.event.VolunteerRegistered;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final Instant START = NOW.plusSeconds(7 * 24 * 3600);
	private static final Instant END = START.plusSeconds(4 * 3600);
	private static final NgoId OWNER = NgoId.generate();

	private Event draft(int capacity) {
		return Event.create(OWNER, EventTitle.of("Mutirão de doação"), Description.of("Arrecadação de alimentos"),
				Location.of("Rua A, 10", City.of("Recife")), Period.of(START, END), Capacity.of(capacity), NOW);
	}

	private Event published(int capacity) {
		Event event = draft(capacity);
		event.publish(NOW);
		event.pullDomainEvents();
		return event;
	}

	@Nested
	class Creation {

		@Test
		void createsDraftAndRaisesEvent() {
			Event event = draft(10);

			assertThat(event.status()).isEqualTo(EventStatus.DRAFT);
			assertThat(event.ownerId()).isEqualTo(OWNER);
			assertThat(event.availableSpots()).isEqualTo(10);
			assertThat(event.pullDomainEvents()).containsExactly(new EventCreated(event.id(), OWNER));
		}

		@Test
		void rejectsEventStartingInThePast() {
			Period past = Period.of(NOW.minusSeconds(3600), NOW.plusSeconds(60));

			assertThatThrownBy(() -> Event.create(OWNER, EventTitle.of("T"), Description.EMPTY,
					Location.of("Rua", City.of("Recife")), past, Capacity.of(5), NOW))
					.isInstanceOf(DomainException.class);
		}

		@Test
		void rejectsMissingOwner() {
			assertThatThrownBy(() -> Event.create(null, EventTitle.of("T"), Description.EMPTY,
					Location.of("Rua", City.of("Recife")), Period.of(START, END), Capacity.of(5), NOW))
					.isInstanceOf(DomainException.class);
		}
	}

	@Nested
	class Lifecycle {

		@Test
		void publishesDraftOnly() {
			Event event = draft(5);
			event.pullDomainEvents();

			event.publish(NOW);

			assertThat(event.status()).isEqualTo(EventStatus.PUBLISHED);
			assertThat(event.pullDomainEvents()).containsExactly(new EventPublished(event.id(), OWNER));
			assertThatThrownBy(() -> event.publish(NOW)).isInstanceOf(DomainException.class);
		}

		@Test
		void cannotPublishAfterStartTime() {
			Event event = draft(5);

			assertThatThrownBy(() -> event.publish(START)).isInstanceOf(DomainException.class);
		}

		@Test
		void cancelNotifiesOnlyActiveVolunteers() {
			Event event = published(5);
			VolunteerId stays = VolunteerId.generate();
			VolunteerId leaves = VolunteerId.generate();
			event.register(stays, NOW);
			event.register(leaves, NOW);
			event.cancelRegistration(leaves, NOW);
			event.pullDomainEvents();

			event.cancel();

			assertThat(event.status()).isEqualTo(EventStatus.CANCELLED);
			assertThat(event.pullDomainEvents()).containsExactly(new EventCancelled(event.id(), List.of(stays)));
		}

		@Test
		void cancelledEventCannotBeChanged() {
			Event event = published(5);
			event.cancel();

			assertThatThrownBy(event::cancel).isInstanceOf(DomainException.class);
			assertThatThrownBy(() -> event.changeCapacity(Capacity.of(9))).isInstanceOf(DomainException.class);
			assertThatThrownBy(() -> event.reschedule(Period.of(START, END), NOW))
					.isInstanceOf(DomainException.class);
			assertThatThrownBy(() -> event.register(VolunteerId.generate(), NOW))
					.isInstanceOf(DomainException.class);
		}

		@Test
		void finishesOnlyAfterItEnded() {
			Event event = published(5);

			assertThatThrownBy(() -> event.finish(END.minusSeconds(1))).isInstanceOf(DomainException.class);
			event.finish(END);

			assertThat(event.status()).isEqualTo(EventStatus.FINISHED);
			assertThat(event.pullDomainEvents()).containsExactly(new EventFinished(event.id()));
		}

		@Test
		void draftCannotBeFinished() {
			assertThatThrownBy(() -> draft(5).finish(END)).isInstanceOf(DomainException.class);
		}

		@Test
		void reschedulesAndReportsAffectedVolunteers() {
			Event event = published(5);
			VolunteerId volunteer = VolunteerId.generate();
			event.register(volunteer, NOW);
			event.pullDomainEvents();
			Period newPeriod = Period.of(START.plusSeconds(86400), END.plusSeconds(86400));

			event.reschedule(newPeriod, NOW);

			assertThat(event.period()).isEqualTo(newPeriod);
			assertThat(event.pullDomainEvents())
					.containsExactly(new EventRescheduled(event.id(), newPeriod, List.of(volunteer)));
		}

		@Test
		void cannotRescheduleToThePast() {
			Event event = published(5);
			Period past = Period.of(NOW.minusSeconds(100), NOW.plusSeconds(100));

			assertThatThrownBy(() -> event.reschedule(past, NOW)).isInstanceOf(DomainException.class);
		}

		@Test
		void updatesDetails() {
			Event event = draft(5);

			event.updateDetails(EventTitle.of("Novo título"), Description.of("Nova"),
					Location.of("Rua B", City.of("Olinda")));

			assertThat(event.title().value()).isEqualTo("Novo título");
			assertThat(event.location().city().value()).isEqualTo("Olinda");
		}
	}

	@Nested
	class Registrations {

		@Test
		void registersVolunteerAndConsumesASpot() {
			Event event = published(2);
			VolunteerId volunteer = VolunteerId.generate();

			event.register(volunteer, NOW);

			assertThat(event.isRegistered(volunteer)).isTrue();
			assertThat(event.availableSpots()).isEqualTo(1);
			assertThat(event.registrations()).hasSize(1);
			assertThat(event.pullDomainEvents()).containsExactly(new VolunteerRegistered(event.id(), OWNER, volunteer));
		}

		@Test
		void cannotRegisterInDraft() {
			assertThatThrownBy(() -> draft(5).register(VolunteerId.generate(), NOW))
					.isInstanceOf(DomainException.class);
		}

		@Test
		void cannotRegisterTwice() {
			Event event = published(5);
			VolunteerId volunteer = VolunteerId.generate();
			event.register(volunteer, NOW);

			assertThatThrownBy(() -> event.register(volunteer, NOW)).isInstanceOf(DomainException.class);
		}

		@Test
		void neverExceedsCapacity() {
			Event event = published(2);
			event.register(VolunteerId.generate(), NOW);
			event.register(VolunteerId.generate(), NOW);

			assertThatThrownBy(() -> event.register(VolunteerId.generate(), NOW))
					.isInstanceOf(DomainException.class)
					.hasMessageContaining("lotado");
			assertThat(event.availableSpots()).isZero();
		}

		@Test
		void cannotRegisterAfterEventStarted() {
			Event event = published(5);

			assertThatThrownBy(() -> event.register(VolunteerId.generate(), START))
					.isInstanceOf(DomainException.class);
		}

		@Test
		void cancellingFreesTheSpotAndAllowsRegisteringAgain() {
			Event event = published(1);
			VolunteerId first = VolunteerId.generate();
			event.register(first, NOW);

			event.cancelRegistration(first, NOW);

			assertThat(event.isRegistered(first)).isFalse();
			assertThat(event.availableSpots()).isEqualTo(1);
			event.register(VolunteerId.generate(), NOW);

			assertThatThrownBy(() -> event.register(first, NOW.plusSeconds(1)))
					.isInstanceOf(DomainException.class)
					.hasMessageContaining("lotado");
		}

		@Test
		void cancelRegistrationRaisesEvent() {
			Event event = published(3);
			VolunteerId volunteer = VolunteerId.generate();
			event.register(volunteer, NOW);
			event.pullDomainEvents();

			event.cancelRegistration(volunteer, NOW);

			assertThat(event.pullDomainEvents())
					.containsExactly(new RegistrationCancelled(event.id(), OWNER, volunteer));
		}

		@Test
		void cannotCancelUnknownRegistrationOrAfterStart() {
			Event event = published(3);
			VolunteerId volunteer = VolunteerId.generate();

			assertThatThrownBy(() -> event.cancelRegistration(volunteer, NOW)).isInstanceOf(DomainException.class);
			event.register(volunteer, NOW);
			assertThatThrownBy(() -> event.cancelRegistration(volunteer, START)).isInstanceOf(DomainException.class);
		}

		@Test
		void recordsAttendanceOnlyAfterStartForRegisteredVolunteers() {
			Event event = published(3);
			VolunteerId volunteer = VolunteerId.generate();
			event.register(volunteer, NOW);
			event.pullDomainEvents();

			assertThatThrownBy(() -> event.recordAttendance(volunteer, NOW)).isInstanceOf(DomainException.class);
			assertThatThrownBy(() -> event.recordAttendance(VolunteerId.generate(), START))
					.isInstanceOf(DomainException.class);

			event.recordAttendance(volunteer, START);

			assertThat(event.registrations().get(0).status()).isEqualTo(RegistrationStatus.ATTENDED);
			assertThat(event.pullDomainEvents()).containsExactly(new AttendanceRecorded(event.id(), volunteer));
			assertThatThrownBy(() -> event.recordAttendance(volunteer, START)).isInstanceOf(DomainException.class);
		}

		@Test
		void attendedVolunteerStillOccupiesTheSpot() {
			Event event = published(1);
			VolunteerId volunteer = VolunteerId.generate();
			event.register(volunteer, NOW);
			event.recordAttendance(volunteer, START);

			assertThat(event.availableSpots()).isZero();
		}
	}

	@Nested
	class CapacityChanges {

		@Test
		void cannotShrinkBelowRegisteredVolunteers() {
			Event event = published(5);
			event.register(VolunteerId.generate(), NOW);
			event.register(VolunteerId.generate(), NOW);

			assertThatThrownBy(() -> event.changeCapacity(Capacity.of(1))).isInstanceOf(DomainException.class);
			event.changeCapacity(Capacity.of(2));

			assertThat(event.capacity()).isEqualTo(Capacity.of(2));
		}
	}

	@Test
	void exposedRegistrationsAreReadOnly() {
		Event event = published(2);

		assertThatThrownBy(() -> event.registrations().add(null)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void restoredEventKeepsStateAndRaisesNoEvents() {
		Event original = published(2);
		VolunteerId volunteer = VolunteerId.generate();
		original.register(volunteer, NOW);

		Event restored = Event.restore(original.id(), OWNER, original.title(), original.description(),
				original.location(), original.period(), original.capacity(), original.status(),
				original.registrations());

		assertThat(restored).isEqualTo(original);
		assertThat(restored.isRegistered(volunteer)).isTrue();
		assertThat(restored.domainEvents()).isEmpty();
	}
}
