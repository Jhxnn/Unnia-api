package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryVolunteerProfileRepository;
import com.Unnia.identity.application.usecase.RegisterVolunteerUseCase.Command;
import com.Unnia.identity.application.usecase.RegisterVolunteerUseCase.Result;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.event.VolunteerProfileCreated;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterVolunteerUseCaseTest {

	private InMemoryAccountRepository accounts;
	private InMemoryVolunteerProfileRepository volunteers;
	private RecordingEventPublisher publisher;
	private RegisterVolunteerUseCase useCase;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		volunteers = new InMemoryVolunteerProfileRepository();
		publisher = new RecordingEventPublisher();
		useCase = new RegisterVolunteerUseCase(accounts, volunteers, new FakePasswordEncoder(), publisher);
	}

	private Command command(String email) {
		return new Command(email, "senha1234", "Maria Silva", "Gosto de ajudar", "Recife");
	}

	@Test
	void registersAccountAndProfileAndPublishesEvents() {
		Result result = useCase.execute(command("maria@exemplo.com"));

		Account account = accounts.findById(result.accountId()).orElseThrow();
		VolunteerProfile profile = volunteers.findById(result.volunteerId()).orElseThrow();
		assertThat(account.role()).isEqualTo(Role.VOLUNTEER);
		assertThat(account.passwordHash().value()).isEqualTo("enc:senha1234");
		assertThat(profile.accountId()).isEqualTo(account.id());
		assertThat(publisher.published()).containsExactly(
				new AccountRegistered(account.id(), Role.VOLUNTEER),
				new VolunteerProfileCreated(profile.id(), account.id()));
	}

	@Test
	void rejectsDuplicatedEmail() {
		useCase.execute(command("maria@exemplo.com"));

		assertThatThrownBy(() -> useCase.execute(command("MARIA@exemplo.com")))
				.isInstanceOf(ConflictException.class);
		assertThat(accounts.size()).isEqualTo(1);
		assertThat(volunteers.size()).isEqualTo(1);
	}

	@Test
	void persistsNothingWhenInputIsInvalid() {
		assertThatThrownBy(() -> useCase.execute(new Command("invalido", "senha1234", "Maria", "", "Recife")))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", "senha1234", " ", "", "Recife")))
				.isInstanceOf(DomainException.class);

		assertThat(accounts.size()).isZero();
		assertThat(volunteers.size()).isZero();
		assertThat(publisher.published()).isEmpty();
	}
}
