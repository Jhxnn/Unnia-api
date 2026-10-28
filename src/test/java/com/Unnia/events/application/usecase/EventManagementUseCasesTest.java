package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventsFixture;
import com.Unnia.events.domain.event.EventCancelled;
import com.Unnia.events.domain.event.EventCreated;
import com.Unnia.events.domain.event.EventFinished;
import com.Unnia.events.domain.event.EventPublished;
import com.Unnia.events.domain.event.EventRescheduled;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.events.domain.model.Period;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.Unnia.events.application.EventsFixture.END;
import static com.Unnia.events.application.EventsFixture.NOW;
import static com.Unnia.events.application.EventsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventManagementUseCasesTest {

	private EventsFixture fx;
	private AccountId ngo;

	@BeforeEach
	void setUp() {
		fx = new EventsFixture();
		ngo = fx.verifiedNgoAccount();
	}

	private Event stored(EventId id) {
		return fx.events.findById(id).orElseThrow();
	}

	@Nested
	class Create {

		private CreateEventUseCase useCase() {
			return new CreateEventUseCase(fx.ngos, fx.events, fx.publisher, fx.clock);
		}

		private CreateEventUseCase.Command command(AccountId account, Instant start, int capacity) {
			return new CreateEventUseCase.Command(account, "Mutirão", "Desc", "Rua A", "Recife", start,
					start.plus(Duration.ofHours(2)), capacity);
		}

		@Test
		void createsDraftOwnedByTheNgoAndPublishesEvent() {
			EventId id = useCase().execute(command(ngo, START, 20));

			Event event = stored(id);
			assertThat(event.status()).isEqualTo(EventStatus.DRAFT);
			assertThat(event.ownerId()).isEqualTo(fx.ngos.findNgoIdByAccount(ngo).orElseThrow());
			assertThat(event.capacity().max()).isEqualTo(20);
			assertThat(fx.publisher.published()).containsExactly(new EventCreated(id, event.ownerId()));
		}

		@Test
		void onlyNgosCanCreateEvents() {
			assertThatThrownBy(() -> useCase().execute(command(fx.volunteerAccount(), START, 5)))
					.isInstanceOf(ForbiddenException.class);
			assertThatThrownBy(() -> useCase().execute(command(AccountId.generate(), START, 5)))
					.isInstanceOf(ForbiddenException.class);
		}

		@Test
		void rejectsInvalidData() {
			assertThatThrownBy(() -> useCase().execute(command(ngo, NOW.minusSeconds(60), 5)))
					.isInstanceOf(DomainException.class);
			assertThatThrownBy(() -> useCase().execute(command(ngo, START, 0))).isInstanceOf(DomainException.class);
			assertThatThrownBy(() -> useCase().execute(new CreateEventUseCase.Command(ngo, " ", "d", "Rua", "Recife",
					START, END, 5))).isInstanceOf(DomainException.class);
			assertThat(fx.events.findByOwner(fx.ngos.findNgoIdByAccount(ngo).orElseThrow())).isEmpty();
			assertThat(fx.publisher.published()).isEmpty();
		}
	}

	@Nested
	class Publish {

		private PublishEventUseCase useCase() {
			return new PublishEventUseCase(fx.ownership, fx.ngos, fx.events, fx.publisher, fx.clock);
		}

		@Test
		void verifiedNgoPublishesItsDraft() {
			EventId id = fx.draftEvent(ngo, 10);

			useCase().execute(new PublishEventUseCase.Command(ngo, id));

			assertThat(stored(id).status()).isEqualTo(EventStatus.PUBLISHED);
			assertThat(fx.publisher.published())
					.containsExactly(new EventPublished(id, stored(id).ownerId()));
		}

		@Test
		void unverifiedNgoCannotPublish() {
			AccountId unverified = fx.unverifiedNgoAccount();
			EventId id = fx.draftEvent(unverified, 10);

			assertThatThrownBy(() -> useCase().execute(new PublishEventUseCase.Command(unverified, id)))
					.isInstanceOf(ForbiddenException.class);
			assertThat(stored(id).status()).isEqualTo(EventStatus.DRAFT);
		}

		@Test
		void anotherNgoCannotPublish() {
			EventId id = fx.draftEvent(ngo, 10);

			assertThatThrownBy(() -> useCase().execute(new PublishEventUseCase.Command(fx.verifiedNgoAccount(), id)))
					.isInstanceOf(ForbiddenException.class);
			assertThatThrownBy(() -> useCase().execute(new PublishEventUseCase.Command(fx.volunteerAccount(), id)))
					.isInstanceOf(ForbiddenException.class);
		}

		@Test
		void failsForUnknownEventAndAlreadyPublishedEvent() {
			assertThatThrownBy(() -> useCase().execute(new PublishEventUseCase.Command(ngo, EventId.generate())))
					.isInstanceOf(NotFoundException.class);
			EventId id = fx.publishedEvent(ngo, 10);
			assertThatThrownBy(() -> useCase().execute(new PublishEventUseCase.Command(ngo, id)))
					.isInstanceOf(DomainException.class);
		}
	}

	@Nested
	class Edit {

		@Test
		void updatesDetails() {
			EventId id = fx.draftEvent(ngo, 10);

			new UpdateEventDetailsUseCase(fx.ownership, fx.events).execute(
					new UpdateEventDetailsUseCase.Command(ngo, id, "Novo título", "Nova", "Rua B", "Olinda"));

			Event event = stored(id);
			assertThat(event.title().value()).isEqualTo("Novo título");
			assertThat(event.location().city().value()).isEqualTo("Olinda");
		}

		@Test
		void onlyTheOwnerCanEdit() {
			EventId id = fx.draftEvent(ngo, 10);

			assertThatThrownBy(() -> new UpdateEventDetailsUseCase(fx.ownership, fx.events).execute(
					new UpdateEventDetailsUseCase.Command(fx.verifiedNgoAccount(), id, "x", "", "Rua", "Recife")))
					.isInstanceOf(ForbiddenException.class);
			assertThat(stored(id).title().value()).isEqualTo("Mutirão de doação");
		}

		@Test
		void reschedulesAndNotifiesRegisteredVolunteers() {
			EventId id = fx.publishedEvent(ngo, 10);
			AccountId volunteerAccount = fx.volunteerAccount();
			new RegisterForEventUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock)
					.execute(new RegisterForEventUseCase.Command(volunteerAccount, id));
			fx.publisher.clear();
			Instant newStart = START.plus(Duration.ofDays(1));
			Instant newEnd = END.plus(Duration.ofDays(1));

			new RescheduleEventUseCase(fx.ownership, fx.events, fx.publisher, fx.clock)
					.execute(new RescheduleEventUseCase.Command(ngo, id, newStart, newEnd));

			VolunteerId volunteerId = fx.volunteers.findVolunteerIdByAccount(volunteerAccount).orElseThrow();
			assertThat(stored(id).period()).isEqualTo(Period.of(newStart, newEnd));
			assertThat(fx.publisher.published()).containsExactly(
					new EventRescheduled(id, Period.of(newStart, newEnd), List.of(volunteerId)));
		}

		@Test
		void cannotRescheduleToThePast() {
			EventId id = fx.publishedEvent(ngo, 10);

			assertThatThrownBy(() -> new RescheduleEventUseCase(fx.ownership, fx.events, fx.publisher, fx.clock)
					.execute(new RescheduleEventUseCase.Command(ngo, id, NOW.minusSeconds(100), NOW.plusSeconds(100))))
					.isInstanceOf(DomainException.class);
		}

		@Test
		void changesCapacityButNeverBelowRegisteredVolunteers() {
			EventId id = fx.publishedEvent(ngo, 5);
			for (int i = 0; i < 3; i++) {
				new RegisterForEventUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock).execute(
						new RegisterForEventUseCase.Command(fx.volunteerAccount(), id));
			}
			ChangeEventCapacityUseCase useCase = new ChangeEventCapacityUseCase(fx.ownership, fx.events);

			assertThatThrownBy(() -> useCase.execute(new ChangeEventCapacityUseCase.Command(ngo, id, 2)))
					.isInstanceOf(DomainException.class);
			useCase.execute(new ChangeEventCapacityUseCase.Command(ngo, id, 3));

			assertThat(stored(id).capacity().max()).isEqualTo(3);
		}
	}

	@Nested
	class Lifecycle {

		@Test
		void cancelsEventAndReportsAffectedVolunteers() {
			EventId id = fx.publishedEvent(ngo, 10);
			AccountId volunteerAccount = fx.volunteerAccount();
			new RegisterForEventUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock)
					.execute(new RegisterForEventUseCase.Command(volunteerAccount, id));
			fx.publisher.clear();

			new CancelEventUseCase(fx.ownership, fx.events, fx.publisher)
					.execute(new CancelEventUseCase.Command(ngo, id));

			VolunteerId volunteerId = fx.volunteers.findVolunteerIdByAccount(volunteerAccount).orElseThrow();
			assertThat(stored(id).status()).isEqualTo(EventStatus.CANCELLED);
			assertThat(fx.publisher.published()).containsExactly(new EventCancelled(id, List.of(volunteerId)));
		}

		@Test
		void onlyTheOwnerCanCancel() {
			EventId id = fx.publishedEvent(ngo, 10);

			assertThatThrownBy(() -> new CancelEventUseCase(fx.ownership, fx.events, fx.publisher)
					.execute(new CancelEventUseCase.Command(fx.verifiedNgoAccount(), id)))
					.isInstanceOf(ForbiddenException.class);
			assertThat(stored(id).status()).isEqualTo(EventStatus.PUBLISHED);
		}

		@Test
		void finishesOnlyAfterTheEventEnded() {
			EventId id = fx.publishedEvent(ngo, 10);
			FinishEventUseCase.Command command = new FinishEventUseCase.Command(ngo, id);

			assertThatThrownBy(() -> new FinishEventUseCase(fx.ownership, fx.events, fx.publisher, fx.clock)
					.execute(command)).isInstanceOf(DomainException.class);

			Clock afterEnd = Clock.fixed(END.plusSeconds(1), ZoneOffset.UTC);
			new FinishEventUseCase(fx.ownership, fx.events, fx.publisher, afterEnd).execute(command);

			assertThat(stored(id).status()).isEqualTo(EventStatus.FINISHED);
			assertThat(fx.publisher.published()).containsExactly(new EventFinished(id));
		}
	}
}
