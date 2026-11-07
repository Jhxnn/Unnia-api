package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.fakes.InMemoryVolunteerProfileRepository;
import com.Unnia.identity.application.usecase.GetCurrentAccountUseCase.View;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCurrentAccountUseCaseTest {

	private InMemoryAccountRepository accounts;
	private InMemoryNgoProfileRepository ngos;
	private InMemoryVolunteerProfileRepository volunteers;
	private GetCurrentAccountUseCase useCase;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		ngos = new InMemoryNgoProfileRepository();
		volunteers = new InMemoryVolunteerProfileRepository();
		useCase = new GetCurrentAccountUseCase(accounts, ngos, volunteers);
	}

	private Account account(String email, Role role) {
		return accounts.save(Account.register(Email.of(email), PasswordHash.of("h"), role));
	}

	@Test
	void ngoSeesItsProfileIdNameAndVerificationStatus() {
		Account account = account("ong@exemplo.com", Role.NGO);
		NgoProfile profile = ngos.save(NgoProfile.create(account, Cnpj.of("11222333000181"),
				DisplayName.of("Mãos Solidárias"), Description.EMPTY, City.of("Recife")));

		View view = useCase.execute(account.id());

		assertThat(view.accountId()).isEqualTo(account.id());
		assertThat(view.email()).isEqualTo("ong@exemplo.com");
		assertThat(view.role()).isEqualTo(Role.NGO);
		assertThat(view.profileId()).isEqualTo(profile.id().value());
		assertThat(view.name()).isEqualTo("Mãos Solidárias");
		assertThat(view.city()).isEqualTo("Recife");
		assertThat(view.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
	}

	@Test
	void ngoVerificationStatusFollowsTheProfile() {
		Account account = account("ong@exemplo.com", Role.NGO);
		NgoProfile profile = ngos.save(NgoProfile.create(account, Cnpj.of("11222333000181"), DisplayName.of("Mãos"),
				Description.EMPTY, City.of("Recife")));
		profile.verify();

		assertThat(useCase.execute(account.id()).verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
	}

	@Test
	void volunteerSeesItsProfileWithoutVerificationStatus() {
		Account account = account("vol@exemplo.com", Role.VOLUNTEER);
		VolunteerProfile profile = volunteers.save(VolunteerProfile.create(account, DisplayName.of("Maria"),
				Description.EMPTY, City.of("Olinda")));

		View view = useCase.execute(account.id());

		assertThat(view.role()).isEqualTo(Role.VOLUNTEER);
		assertThat(view.profileId()).isEqualTo(profile.id().value());
		assertThat(view.name()).isEqualTo("Maria");
		assertThat(view.city()).isEqualTo("Olinda");
		assertThat(view.verificationStatus()).isNull();
	}

	@Test
	void adminHasNoProfile() {
		Account admin = account("admin@unnia.com", Role.ADMIN);

		View view = useCase.execute(admin.id());

		assertThat(view.role()).isEqualTo(Role.ADMIN);
		assertThat(view.email()).isEqualTo("admin@unnia.com");
		assertThat(view.profileId()).isNull();
		assertThat(view.name()).isNull();
		assertThat(view.verificationStatus()).isNull();
	}

	@Test
	void anAccountWhoseProfileIsMissingStillAnswers() {
		Account account = account("ong@exemplo.com", Role.NGO);

		View view = useCase.execute(account.id());

		assertThat(view.accountId()).isEqualTo(account.id());
		assertThat(view.profileId()).isNull();
	}

	@Test
	void unknownAccountIsNotFound() {
		assertThatThrownBy(() -> useCase.execute(AccountId.generate())).isInstanceOf(NotFoundException.class);
	}
}
