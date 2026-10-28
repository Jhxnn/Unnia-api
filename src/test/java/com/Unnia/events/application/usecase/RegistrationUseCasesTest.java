package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventsFixture;
import com.Unnia.events.domain.event.AttendanceRecorded;
import com.Unnia.events.domain.event.RegistrationCancelled;
import com.Unnia.events.domain.event.VolunteerRegistered;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.RegistrationStatus;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.Unnia.events.application.EventsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationUseCasesTest {

	private EventsFixture fx;
	private AccountId ngo;
	private EventId eventId;
	private AccountId volunteer;

	@BeforeEach
	void setUp() {
		fx = new EventsFixture();
		ngo = fx.verifiedNgoAccount();
		eventId = fx.publishedEvent(ngo, 2);
		volunteer = fx.volunteerAccount();
	}

	private RegisterForEventUseCase register() {
		return new RegisterForEventUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock);
	}

	private CancelRegistrationUseCase cancel() {
		return new CancelRegistrationUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock);
	}

	private Event stored() {
		return fx.events.findById(eventId).orElseThrow();
	}

	private VolunteerId volunteerId(AccountId account) {
		return fx.volunteers.findVolunteerIdByAccount(account).orElseThrow();
	}

	@Test
	void volunteerRegistersAndEventIsPublished() {
		register().execute(new RegisterForEventUseCase.Command(volunteer, eventId));

		NgoId owner = stored().ownerId();
		assertThat(stored().isRegistered(volunteerId(volunteer))).isTrue();
		assertThat(stored().availableSpots()).isEqualTo(1);
		assertThat(fx.publisher.published())
				.containsExactly(new VolunteerRegistered(eventId, owner, volunteerId(volunteer)));
	}

	@Test
	void onlyVolunteersCanRegister() {
		assertThatThrownBy(() -> register().execute(new RegisterForEventUseCase.Command(ngo, eventId)))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> register().execute(new RegisterForEventUseCase.Command(AccountId.generate(),
				eventId))).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void cannotRegisterTwiceOrBeyondCapacityOrInUnknownEvent() {
		register().execute(new RegisterForEventUseCase.Command(volunteer, eventId));
		register().execute(new RegisterForEventUseCase.Command(fx.volunteerAccount(), eventId));

		assertThatThrownBy(() -> register().execute(new RegisterForEventUseCase.Command(volunteer, eventId)))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> register().execute(new RegisterForEventUseCase.Command(fx.volunteerAccount(),
				eventId))).isInstanceOf(DomainException.class).hasMessageContaining("lotado");
		assertThatThrownBy(() -> register().execute(new RegisterForEventUseCase.Command(volunteer,
				EventId.generate()))).isInstanceOf(NotFoundException.class);
		assertThat(stored().occupiedSpots()).isEqualTo(2);
	}

	@Test
	void cannotRegisterInDraftEvent() {
		EventId draft = fx.draftEvent(ngo, 5);

		assertThatThrownBy(() -> register().execute(new RegisterForEventUseCase.Command(volunteer, draft)))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void volunteerCancelsRegistrationAndFreesTheSpot() {
		register().execute(new RegisterForEventUseCase.Command(volunteer, eventId));
		fx.publisher.clear();

		cancel().execute(new CancelRegistrationUseCase.Command(volunteer, eventId));

		assertThat(stored().isRegistered(volunteerId(volunteer))).isFalse();
		assertThat(stored().availableSpots()).isEqualTo(2);
		assertThat(fx.publisher.published())
				.containsExactly(new RegistrationCancelled(eventId, stored().ownerId(), volunteerId(volunteer)));
	}

	@Test
	void cancellingWithoutRegistrationOrAsNgoFails() {
		assertThatThrownBy(() -> cancel().execute(new CancelRegistrationUseCase.Command(volunteer, eventId)))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> cancel().execute(new CancelRegistrationUseCase.Command(ngo, eventId)))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void ngoRecordsAttendanceOnlyAfterTheEventStarted() {
		register().execute(new RegisterForEventUseCase.Command(volunteer, eventId));
		fx.publisher.clear();
		RecordAttendanceUseCase.Command command =
				new RecordAttendanceUseCase.Command(ngo, eventId, volunteerId(volunteer));

		assertThatThrownBy(() -> new RecordAttendanceUseCase(fx.ownership, fx.events, fx.publisher, fx.clock)
				.execute(command)).isInstanceOf(DomainException.class);

		Clock duringEvent = Clock.fixed(START.plusSeconds(60), ZoneOffset.UTC);
		new RecordAttendanceUseCase(fx.ownership, fx.events, fx.publisher, duringEvent).execute(command);

		assertThat(stored().registrations().get(0).status()).isEqualTo(RegistrationStatus.ATTENDED);
		assertThat(fx.publisher.published()).containsExactly(new AttendanceRecorded(eventId, volunteerId(volunteer)));
	}

	@Test
	void onlyTheOwnerNgoRecordsAttendance() {
		register().execute(new RegisterForEventUseCase.Command(volunteer, eventId));
		Clock duringEvent = Clock.fixed(START.plusSeconds(60), ZoneOffset.UTC);
		RecordAttendanceUseCase useCase = new RecordAttendanceUseCase(fx.ownership, fx.events, fx.publisher, duringEvent);

		assertThatThrownBy(() -> useCase.execute(
				new RecordAttendanceUseCase.Command(fx.verifiedNgoAccount(), eventId, volunteerId(volunteer))))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(
				new RecordAttendanceUseCase.Command(volunteer, eventId, volunteerId(volunteer))))
				.isInstanceOf(ForbiddenException.class);
	}
}
