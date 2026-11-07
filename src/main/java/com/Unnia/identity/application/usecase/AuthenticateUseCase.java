package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.LoginAttempts;
import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.TooManyRequestsException;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Locale;

public class AuthenticateUseCase {

	private static final String INVALID_CREDENTIALS = "Credenciais inválidas";
	private static final int MAX_KEY_LENGTH = 254;

	public record Command(String email, String password) {
	}

	public record Result(AccountId accountId, Role role) {
	}

	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;
	private final LoginAttempts attempts;

	public AuthenticateUseCase(AccountRepository accounts, PasswordEncoder passwordEncoder, LoginAttempts attempts) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
		this.attempts = attempts;
	}

	public Result execute(Command command) {
		String key = attemptKey(command.email());
		attempts.blockedFor(key).ifPresent(wait -> {
			throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente mais tarde.", wait);
		});

		Account account;
		try {
			account = findAccount(command.email());
			if (command.password() == null || !passwordEncoder.matches(command.password(), account.passwordHash())) {
				throw new UnauthorizedException(INVALID_CREDENTIALS);
			}
		} catch (UnauthorizedException e) {
			// e-mail inexistente e senha errada contam igual, para não revelar quais contas existem
			attempts.recordFailure(key);
			throw e;
		}
		attempts.reset(key);

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

	/** Mesma normalização do {@link Email}, com tamanho limitado para a chave não virar vetor de memória. */
	private String attemptKey(String rawEmail) {
		if (rawEmail == null) {
			return "";
		}
		String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
		return normalized.length() > MAX_KEY_LENGTH ? normalized.substring(0, MAX_KEY_LENGTH) : normalized;
	}
}
