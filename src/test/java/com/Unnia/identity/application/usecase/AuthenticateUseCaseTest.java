package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.FakeLoginAttempts;
import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.usecase.AuthenticateUseCase.Command;
import com.Unnia.identity.application.usecase.AuthenticateUseCase.Result;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.TooManyRequestsException;
import com.Unnia.shared.application.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateUseCaseTest {

	private InMemoryAccountRepository accounts;
	private FakeLoginAttempts attempts;
	private AuthenticateUseCase useCase;
	private Account account;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		attempts = new FakeLoginAttempts(3);
		useCase = new AuthenticateUseCase(accounts, new FakePasswordEncoder(), attempts);
		account = accounts.save(Account.register(Email.of("maria@exemplo.com"), PasswordHash.of("enc:senha1234"),
				Role.VOLUNTEER));
	}

	private void fail(String email) {
		assertThatThrownBy(() -> useCase.execute(new Command(email, "errada1234")))
				.isInstanceOf(UnauthorizedException.class);
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

	@Test
	void blocksTheEmailAfterTooManyFailuresEvenWithTheCorrectPassword() {
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");

		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", "senha1234")))
				.isInstanceOfSatisfying(TooManyRequestsException.class,
						e -> assertThat(e.retryAfter()).isEqualTo(FakeLoginAttempts.BLOCK));
	}

	@Test
	void failuresOnUnknownEmailsCountToo_soAccountExistenceIsNotRevealed() {
		fail("fantasma@exemplo.com");
		fail("fantasma@exemplo.com");
		fail("fantasma@exemplo.com");

		assertThatThrownBy(() -> useCase.execute(new Command("fantasma@exemplo.com", "qualquer1")))
				.isInstanceOf(TooManyRequestsException.class);
	}

	@Test
	void emailVariantsShareTheSameCounter() {
		fail("maria@exemplo.com");
		fail("MARIA@exemplo.com");
		fail("  Maria@Exemplo.com ");

		assertThatThrownBy(() -> useCase.execute(new Command("maria@EXEMPLO.com", "senha1234")))
				.isInstanceOf(TooManyRequestsException.class);
	}

	@Test
	void aSuccessfulLoginClearsTheFailures() {
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");

		useCase.execute(new Command("maria@exemplo.com", "senha1234"));

		assertThat(attempts.failuresOf("maria@exemplo.com")).isZero();
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");
		assertThat(useCase.execute(new Command("maria@exemplo.com", "senha1234")).accountId()).isEqualTo(account.id());
	}

	@Test
	void otherEmailsAreNotAffectedByTheBlock() {
		Account other = accounts.save(Account.register(Email.of("joao@exemplo.com"), PasswordHash.of("enc:senha1234"),
				Role.NGO));
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");

		assertThat(useCase.execute(new Command("joao@exemplo.com", "senha1234")).accountId()).isEqualTo(other.id());
	}

	@Test
	void aBlockedAttemptIsNotCountedAsAnotherFailure() {
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");
		fail("maria@exemplo.com");

		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", "errada1234")))
				.isInstanceOf(TooManyRequestsException.class);

		assertThat(attempts.failuresOf("maria@exemplo.com")).isEqualTo(3);
	}

	@Test
	void aSuspendedAccountWithTheRightPasswordDoesNotCountAsAFailure() {
		account.suspend();

		assertThatThrownBy(() -> useCase.execute(new Command("maria@exemplo.com", "senha1234")))
				.isInstanceOf(UnauthorizedException.class);

		assertThat(attempts.failuresOf("maria@exemplo.com")).isZero();
	}
}
