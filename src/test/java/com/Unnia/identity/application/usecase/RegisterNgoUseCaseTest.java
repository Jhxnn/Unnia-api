package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.fakes.InMemoryNgoProfileRepository;
import com.Unnia.identity.application.usecase.RegisterNgoUseCase.Command;
import com.Unnia.identity.application.usecase.RegisterNgoUseCase.Result;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.event.NgoProfileCreated;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterNgoUseCaseTest {

	private InMemoryAccountRepository accounts;
	private InMemoryNgoProfileRepository ngos;
	private RecordingEventPublisher publisher;
	private RegisterNgoUseCase useCase;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		ngos = new InMemoryNgoProfileRepository();
		publisher = new RecordingEventPublisher();
		useCase = new RegisterNgoUseCase(accounts, ngos, new FakePasswordEncoder(), publisher);
	}

	private Command command(String email, String cnpj) {
		return new Command(email, "senha1234", cnpj, "Mãos Solidárias", "Ajudamos famílias", "Recife");
	}

	@Test
	void registersAccountAndProfileAndPublishesEvents() {
		Result result = useCase.execute(command("ONG@exemplo.com", "11.222.333/0001-81"));

		Account account = accounts.findById(result.accountId()).orElseThrow();
		NgoProfile profile = ngos.findById(result.ngoId()).orElseThrow();
		assertThat(account.role()).isEqualTo(Role.NGO);
		assertThat(account.email().value()).isEqualTo("ong@exemplo.com");
		assertThat(account.passwordHash().value()).isEqualTo("enc:senha1234");
		assertThat(profile.accountId()).isEqualTo(account.id());
		assertThat(publisher.published()).containsExactly(
				new AccountRegistered(account.id(), Role.NGO),
				new NgoProfileCreated(profile.id(), account.id()));
	}

	@Test
	void rejectsDuplicatedEmail() {
		useCase.execute(command("ong@exemplo.com", "11222333000181"));

		assertThatThrownBy(() -> useCase.execute(command("ONG@exemplo.com", "45723174000110")))
				.isInstanceOf(ConflictException.class);
		assertThat(accounts.size()).isEqualTo(1);
	}

	@Test
	void rejectsDuplicatedCnpj() {
		useCase.execute(command("a@exemplo.com", "11222333000181"));

		assertThatThrownBy(() -> useCase.execute(command("b@exemplo.com", "11.222.333/0001-81")))
				.isInstanceOf(ConflictException.class);
		assertThat(ngos.size()).isEqualTo(1);
	}

	@Test
	void persistsNothingWhenInputIsInvalid() {
		assertThatThrownBy(() -> useCase.execute(command("ong@exemplo.com", "123")))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(new Command("ong@exemplo.com", "fraca", "11222333000181", "N", "",
				"Recife"))).isInstanceOf(DomainException.class);

		assertThat(accounts.size()).isZero();
		assertThat(ngos.size()).isZero();
		assertThat(publisher.published()).isEmpty();
	}
}
