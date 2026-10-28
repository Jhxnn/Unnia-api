package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.fakes.InMemoryVolunteerProfileRepository;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileUseCasesTest {

	private InMemoryNgoProfileRepository ngos;
	private InMemoryVolunteerProfileRepository volunteers;
	private NgoProfile ngo;
	private VolunteerProfile volunteer;

	@BeforeEach
	void setUp() {
		ngos = new InMemoryNgoProfileRepository();
		volunteers = new InMemoryVolunteerProfileRepository();
		Account ngoAccount = Account.register(Email.of("ong@exemplo.com"), PasswordHash.of("h"), Role.NGO);
		Account volunteerAccount = Account.register(Email.of("vol@exemplo.com"), PasswordHash.of("h"),
				Role.VOLUNTEER);
		ngo = ngos.save(NgoProfile.create(ngoAccount, Cnpj.of("11222333000181"), DisplayName.of("Mãos"),
				Description.of("Desc"), City.of("Recife")));
		volunteer = volunteers.save(VolunteerProfile.create(volunteerAccount, DisplayName.of("Maria"),
				Description.of("Bio"), City.of("Recife")));
	}

	@Test
	void updatesNgoProfile() {
		new UpdateNgoProfileUseCase(ngos).execute(new UpdateNgoProfileUseCase.Command(ngo.accountId(),
				"Novo Nome", "Nova desc", "Olinda"));

		NgoProfile saved = ngos.findById(ngo.id()).orElseThrow();
		assertThat(saved.name().value()).isEqualTo("Novo Nome");
		assertThat(saved.city().value()).isEqualTo("Olinda");
	}

	@Test
	void updateNgoProfileFailsForUnknownAccountOrInvalidData() {
		UpdateNgoProfileUseCase useCase = new UpdateNgoProfileUseCase(ngos);

		assertThatThrownBy(() -> useCase.execute(
				new UpdateNgoProfileUseCase.Command(AccountId.generate(), "N", "", "Recife")))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> useCase.execute(
				new UpdateNgoProfileUseCase.Command(ngo.accountId(), " ", "", "Recife")))
				.isInstanceOf(DomainException.class);
		assertThat(ngos.findById(ngo.id()).orElseThrow().name().value()).isEqualTo("Mãos");
	}

	@Test
	void updatesVolunteerProfile() {
		new UpdateVolunteerProfileUseCase(volunteers).execute(new UpdateVolunteerProfileUseCase.Command(
				volunteer.accountId(), "Maria Souza", "Nova bio", "Olinda"));

		VolunteerProfile saved = volunteers.findById(volunteer.id()).orElseThrow();
		assertThat(saved.name().value()).isEqualTo("Maria Souza");
		assertThat(saved.bio().value()).isEqualTo("Nova bio");
	}

	@Test
	void updateVolunteerProfileFailsForUnknownAccount() {
		assertThatThrownBy(() -> new UpdateVolunteerProfileUseCase(volunteers).execute(
				new UpdateVolunteerProfileUseCase.Command(AccountId.generate(), "N", "", "Recife")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void getsNgoProfileView() {
		GetNgoProfileUseCase.View view = new GetNgoProfileUseCase(ngos).execute(ngo.id());

		assertThat(view.name()).isEqualTo("Mãos");
		assertThat(view.cnpj()).isEqualTo("11.222.333/0001-81");
		assertThat(view.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void getsVolunteerProfileView() {
		GetVolunteerProfileUseCase.View view = new GetVolunteerProfileUseCase(volunteers).execute(volunteer.id());

		assertThat(view.name()).isEqualTo("Maria");
		assertThat(view.bio()).isEqualTo("Bio");
		assertThat(view.city()).isEqualTo("Recife");
	}

	@Test
	void queriesFailForUnknownIds() {
		assertThatThrownBy(() -> new GetNgoProfileUseCase(ngos).execute(NgoId.generate()))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> new GetVolunteerProfileUseCase(volunteers).execute(VolunteerId.generate()))
				.isInstanceOf(NotFoundException.class);
	}
}
