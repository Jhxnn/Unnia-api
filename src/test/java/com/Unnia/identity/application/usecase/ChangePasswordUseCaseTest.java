package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.fakes.FakePasswordEncoder;
import com.Unnia.identity.application.fakes.InMemoryAccountRepository;
import com.Unnia.identity.application.usecase.ChangePasswordUseCase.Command;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangePasswordUseCaseTest {

	private InMemoryAccountRepository accounts;
	private ChangePasswordUseCase useCase;
	private Account account;

	@BeforeEach
	void setUp() {
		accounts = new InMemoryAccountRepository();
		useCase = new ChangePasswordUseCase(accounts, new FakePasswordEncoder());
		account = accounts.save(Account.register(Email.of("maria@exemplo.com"), PasswordHash.of("enc:senha1234"),
				Role.VOLUNTEER));
	}

	@Test
	void changesPasswordWhenCurrentOneMatches() {
		useCase.execute(new Command(account.id(), "senha1234", "novaSenha99"));

		assertThat(accounts.findById(account.id()).orElseThrow().passwordHash().value())
				.isEqualTo("enc:novaSenha99");
	}

	@Test
	void rejectsWrongCurrentPasswordAndKeepsTheOldOne() {
		assertThatThrownBy(() -> useCase.execute(new Command(account.id(), "errada123", "novaSenha99")))
				.isInstanceOf(UnauthorizedException.class);

		assertThat(account.passwordHash().value()).isEqualTo("enc:senha1234");
	}

	@Test
	void rejectsWeakNewPassword() {
		assertThatThrownBy(() -> useCase.execute(new Command(account.id(), "senha1234", "fraca")))
				.isInstanceOf(DomainException.class);

		assertThat(account.passwordHash().value()).isEqualTo("enc:senha1234");
	}

	@Test
	void failsForUnknownAccount() {
		assertThatThrownBy(() -> useCase.execute(new Command(AccountId.generate(), "senha1234", "novaSenha99")))
				.isInstanceOf(NotFoundException.class);
	}
}
