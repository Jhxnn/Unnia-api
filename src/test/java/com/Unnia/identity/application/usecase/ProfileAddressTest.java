package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.fakes.InMemoryVolunteerProfileRepository;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Estado e CEP nos perfis: opcionais, validados quando informados e preservados quando não são enviados. */
class ProfileAddressTest {

	private InMemoryAccountRepository accounts;
	private InMemoryNgoProfileRepository ngos;
	private InMemoryVolunteerProfileRepository volunteers;
	private RecordingEventPublisher publisher;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		ngos = new InMemoryNgoProfileRepository();
		volunteers = new InMemoryVolunteerProfileRepository();
		publisher = new RecordingEventPublisher();
	}

	private RegisterVolunteerUseCase.Result registerVolunteer(String state, String cep) {
		return new RegisterVolunteerUseCase(accounts, volunteers, new FakePasswordEncoder(), publisher).execute(
				new RegisterVolunteerUseCase.Command("maria@exemplo.com", "senha1234", "Maria", "Bio", "Recife", state,
						cep));
	}

	private RegisterNgoUseCase.Result registerNgo(String state, String cep) {
		return new RegisterNgoUseCase(accounts, ngos, new FakePasswordEncoder(), publisher).execute(
				new RegisterNgoUseCase.Command("ong@exemplo.com", "senha1234", "11.222.333/0001-81", "Mãos",
						"Desc", "Recife", state, cep));
	}

	@Test
	void aVolunteerRegistersWithStateAndCep() {
		VolunteerProfile profile = volunteers.findById(registerVolunteer("pe", "50030-230").volunteerId()).orElseThrow();

		assertThat(profile.state().orElseThrow().value()).isEqualTo("PE");
		assertThat(profile.cep().orElseThrow().formatted()).isEqualTo("50030-230");
	}

	@Test
	void aVolunteerRegistersWithoutThem() {
		VolunteerProfile profile = volunteers.findById(registerVolunteer(null, null).volunteerId()).orElseThrow();

		assertThat(profile.state()).isEmpty();
		assertThat(profile.cep()).isEmpty();
	}

	@Test
	void anNgoRegistersWithStateAndCep() {
		NgoProfile profile = ngos.findById(registerNgo("PE", "50030230").ngoId()).orElseThrow();

		assertThat(profile.state().orElseThrow().value()).isEqualTo("PE");
		assertThat(profile.cep().orElseThrow().value()).isEqualTo("50030230");
	}

	@Test
	void invalidStateOrCepStopsTheRegistrationBeforeAnythingIsSaved() {
		assertThatThrownBy(() -> registerVolunteer("XX", "50030-230")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> registerVolunteer("PE", "123")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> registerNgo("PE", "abc")).isInstanceOf(DomainException.class);

		assertThat(accounts.size()).isZero();
		assertThat(volunteers.size()).isZero();
		assertThat(ngos.size()).isZero();
	}

	@Test
	void updatingAProfileCanChangeTheAddress() {
		VolunteerProfile before = volunteers.findById(registerVolunteer("PE", "50030-230").volunteerId()).orElseThrow();

		new UpdateVolunteerProfileUseCase(volunteers).execute(new UpdateVolunteerProfileUseCase.Command(
				before.accountId(), "Maria", "Bio", "São Paulo", "SP", "01310-100"));

		VolunteerProfile after = volunteers.findById(before.id()).orElseThrow();
		assertThat(after.city().value()).isEqualTo("São Paulo");
		assertThat(after.state().orElseThrow().value()).isEqualTo("SP");
		assertThat(after.cep().orElseThrow().formatted()).isEqualTo("01310-100");
	}

	@Test
	void updatingWithoutStateAndCepKeepsTheOnesTheProfileAlreadyHas() {
		NgoProfile before = ngos.findById(registerNgo("PE", "50030-230").ngoId()).orElseThrow();

		new UpdateNgoProfileUseCase(ngos).execute(
				new UpdateNgoProfileUseCase.Command(before.accountId(), "Novo nome", "Desc", "Olinda"));

		NgoProfile after = ngos.findById(before.id()).orElseThrow();
		assertThat(after.name().value()).isEqualTo("Novo nome");
		assertThat(after.state().orElseThrow().value()).isEqualTo("PE");
		assertThat(after.cep().orElseThrow().value()).isEqualTo("50030230");
	}

	@Test
	void sendingBlankStateAndCepClearsThem() {
		VolunteerProfile before = volunteers.findById(registerVolunteer("PE", "50030-230").volunteerId()).orElseThrow();

		new UpdateVolunteerProfileUseCase(volunteers).execute(new UpdateVolunteerProfileUseCase.Command(
				before.accountId(), "Maria", "Bio", "Recife", "", ""));

		VolunteerProfile after = volunteers.findById(before.id()).orElseThrow();
		assertThat(after.state()).isEmpty();
		assertThat(after.cep()).isEmpty();
	}

	@Test
	void theProfileViewsExposeStateAndTheFormattedCep() {
		NgoProfile ngo = ngos.findById(registerNgo("PE", "50030230").ngoId()).orElseThrow();
		VolunteerProfile volunteer = volunteers.findById(registerVolunteer("sp", "01310100").volunteerId()).orElseThrow();

		GetNgoProfileUseCase.View ngoView = new GetNgoProfileUseCase(ngos).execute(ngo.id());
		GetVolunteerProfileUseCase.View volunteerView = new GetVolunteerProfileUseCase(volunteers).execute(volunteer.id());

		assertThat(ngoView.state()).isEqualTo("PE");
		assertThat(ngoView.cep()).isEqualTo("50030-230");
		assertThat(volunteerView.state()).isEqualTo("SP");
		assertThat(volunteerView.cep()).isEqualTo("01310-100");
	}

	@Test
	void profilesWithoutAddressDataShowNulls() {
		NgoProfile ngo = ngos.findById(registerNgo(null, null).ngoId()).orElseThrow();

		GetNgoProfileUseCase.View view = new GetNgoProfileUseCase(ngos).execute(ngo.id());

		assertThat(view.state()).isNull();
		assertThat(view.cep()).isNull();
	}
}
