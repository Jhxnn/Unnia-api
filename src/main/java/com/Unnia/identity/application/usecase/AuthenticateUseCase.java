package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;

public class AuthenticateUseCase {

	private static final String INVALID_CREDENTIALS = "Credenciais inválidas";

	public record Command(String email, String password) {
	}

	public record Result(AccountId accountId, Role role) {
	}

	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;

	public AuthenticateUseCase(AccountRepository accounts, PasswordEncoder passwordEncoder) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
	}

	public Result execute(Command command) {
		Account account = findAccount(command.email());
		if (command.password() == null || !passwordEncoder.matches(command.password(), account.passwordHash())) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
		if (!account.isActive()) {
			throw new UnauthorizedException("Conta suspensa");
		}
		return new Result(account.id(), account.role());
	}

	/** E-mail malformado ou inexistente geram a mesma resposta, para não revelar quais contas existem. */
	private Account findAccount(String rawEmail) {
		try {
			return accounts.findByEmail(Email.of(rawEmail))
					.orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
		} catch (DomainException e) {
			throw new UnauthorizedException(INVALID_CREDENTIALS);
		}
	}
}
