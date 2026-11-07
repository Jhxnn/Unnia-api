package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventsFixture;
import com.Unnia.events.application.usecase.ListEventRegistrationsUseCase.Registrant;
import com.Unnia.events.domain.model.RegistrationStatus;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;

import static com.Unnia.events.application.EventsFixture.NOW;
import static com.Unnia.events.application.EventsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListEventRegistrationsUseCaseTest {

	private EventsFixture fx;
	private AccountId ngo;
	private EventId eventId;
	private ListEventRegistrationsUseCase useCase;

	@BeforeEach
	void setUp() {
		fx = new EventsFixture();
		ngo = fx.verifiedNgoAccount();
		eventId = fx.publishedEvent(ngo, 10);
		useCase = new ListEventRegistrationsUseCase(fx.ownership, fx.volunteers);
	}

	/** Inscreve um voluntário novo em um instante específico, para a ordem de inscrição ser verificável. */
	private AccountId volunteerRegisteredAt(String name, String city, Duration afterNow) {
		AccountId account = AccountId.generate();
		fx.volunteers.add(account, name, city);
		new RegisterForEventUseCase(fx.volunteers, fx.events, fx.publisher,
				Clock.fixed(NOW.plus(afterNow), ZoneOffset.UTC))
				.execute(new RegisterForEventUseCase.Command(account, eventId));
		return account;
	}

	@Test
	void theOwnerSeesWhoIsRegisteredWithNameAndCity() {
		volunteerRegisteredAt("Maria Souza", "Recife", Duration.ofMinutes(1));

		List<Registrant> registrants = useCase.execute(ngo, eventId);

		assertThat(registrants).singleElement().satisfies(registrant -> {
			assertThat(registrant.name()).isEqualTo("Maria Souza");
			assertThat(registrant.city()).isEqualTo("Recife");
			assertThat(registrant.status()).isEqualTo(RegistrationStatus.CONFIRMED);
			assertThat(registrant.registeredAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
		});
	}

	@Test
	void registrantsAreOrderedFromTheOldestRegistration() {
		volunteerRegisteredAt("Segundo", "Olinda", Duration.ofMinutes(20));
		volunteerRegisteredAt("Primeiro", "Recife", Duration.ofMinutes(5));
		volunteerRegisteredAt("Terceiro", "Recife", Duration.ofMinutes(40));

		assertThat(useCase.execute(ngo, eventId)).extracting(Registrant::name)
				.containsExactly("Primeiro", "Segundo", "Terceiro");
	}

	@Test
	void cancelledRegistrationsAreLeftOut() {
		AccountId stays = volunteerRegisteredAt("Fica", "Recife", Duration.ofMinutes(1));
		AccountId leaves = volunteerRegisteredAt("Sai", "Recife", Duration.ofMinutes(2));
		new CancelRegistrationUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock)
				.execute(new CancelRegistrationUseCase.Command(leaves, eventId));

		assertThat(useCase.execute(ngo, eventId)).extracting(Registrant::name).containsExactly("Fica");
		assertThat(stays).isNotNull();
	}

	@Test
	void volunteersWhoAttendedStayInTheListAsAttended() {
		AccountId account = volunteerRegisteredAt("Presente", "Recife", Duration.ofMinutes(1));
		VolunteerId volunteerId = fx.volunteers.findVolunteerIdByAccount(account).orElseThrow();
		new RecordAttendanceUseCase(fx.ownership, fx.events, fx.publisher,
				Clock.fixed(START.plusSeconds(60), ZoneOffset.UTC))
				.execute(new RecordAttendanceUseCase.Command(ngo, eventId, volunteerId));

		assertThat(useCase.execute(ngo, eventId)).singleElement()
				.satisfies(registrant -> {
					assertThat(registrant.status()).isEqualTo(RegistrationStatus.ATTENDED);
					assertThat(registrant.volunteerId()).isEqualTo(volunteerId);
				});
	}

	@Test
	void anEventWithNoRegistrationsGivesAnEmptyList() {
		assertThat(useCase.execute(ngo, eventId)).isEmpty();
	}

	@Test
	void aVolunteerWhoseProfileIsGoneStillAppearsWithAGenericName() {
		AccountId account = volunteerRegisteredAt("Some", "Recife", Duration.ofMinutes(1));
		fx.volunteers.forget(fx.volunteers.findVolunteerIdByAccount(account).orElseThrow());

		assertThat(useCase.execute(ngo, eventId)).singleElement().satisfies(registrant -> {
			assertThat(registrant.name()).isEqualTo(ListEventRegistrationsUseCase.UNKNOWN_VOLUNTEER);
			assertThat(registrant.city()).isEmpty();
		});
	}

	@Test
	void onlyTheOwnerNgoCanSeeTheList() {
		volunteerRegisteredAt("Maria", "Recife", Duration.ofMinutes(1));

		assertThatThrownBy(() -> useCase.execute(fx.verifiedNgoAccount(), eventId)).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(fx.volunteerAccount(), eventId)).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(AccountId.generate(), eventId)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void anUnknownEventIsNotFound() {
		assertThatThrownBy(() -> useCase.execute(ngo, EventId.generate())).isInstanceOf(NotFoundException.class);
	}
}
