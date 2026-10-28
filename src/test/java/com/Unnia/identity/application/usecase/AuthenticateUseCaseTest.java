package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.usecase.AuthenticateUseCase.Command;
import com.Unnia.identity.application.usecase.AuthenticateUseCase.Result;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateUseCaseTest {

	private InMemoryAccountRepository accounts;
	private AuthenticateUseCase useCase;
	private Account account;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		useCase = new AuthenticateUseCase(accounts, new FakePasswordEncoder());
		account = accounts.save(Account.register(Email.of("maria@exemplo.com"), PasswordHash.of("enc:senha1234"),
				Role.VOLUNTEER));
	}

	@Test
	void authenticatesWithCorrectCredentials() {
		Result result = useCase.execute(new Command("Maria@Exemplo.com", "senha1234"));

		assertThat(result.accountId()).isEqualTo(account.id());
		assertThat(result.role()).isEqualTo(Role.VOLUNTEER);
	}

	@Test
	void wrongPasswordUnknownEmailAndMalformedEmailLookTheSame() {
		String message = "Credenciais inválidas";

		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", "errada123")))
				.isInstanceOf(UnauthorizedException.class).hasMessage(message);
		assertThatThrownBy(() -> useCase.execute(new Command("outra@exemplo.com", "senha1234")))
				.isInstanceOf(UnauthorizedException.class).hasMessage(message);
		assertThatThrownBy(() -> useCase.execute(new Command("nao-e-email", "senha1234")))
				.isInstanceOf(UnauthorizedException.class).hasMessage(message);
		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", null)))
				.isInstanceOf(UnauthorizedException.class).hasMessage(message);
	}

	@Test
	void suspendedAccountCannotLogIn() {
		account.suspend();

		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", "senha1234")))
				.isInstanceOf(UnauthorizedException.class);
	}
}
