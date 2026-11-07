package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.TokenHasher;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.EmailVerificationTokenRepository;
import com.Unnia.shared.domain.DomainException;

import java.time.Clock;

/**
 * Confirma o e-mail a partir do link recebido. Abrir de novo um link que já confirmou a conta não é erro;
 * um link vencido, desconhecido ou substituído por outro mais novo é recusado.
 */
public class VerifyEmailUseCase {

	public record Command(String token) {
	}

	private final AccountRepository accounts;
	private final EmailVerificationTokenRepository tokens;
	private final TokenHasher hasher;
	private final Clock clock;

	public VerifyEmailUseCase(AccountRepository accounts, EmailVerificationTokenRepository tokens, TokenHasher hasher,
			Clock clock) {
		this.accounts = accounts;
		this.tokens = tokens;
		this.hasher = hasher;
		this.clock = clock;
	}

	public void execute(Command command) {
		if (command.token() == null || command.token().isBlank()) {
			throw new DomainException(EmailVerificationToken.INVALID_LINK);
		}
		EmailVerificationToken token = tokens.findByTokenHash(hasher.hash(command.token().trim()))
				.orElseThrow(() -> new DomainException(EmailVerificationToken.INVALID_LINK));
		Account account = accounts.findById(token.accountId())
				.orElseThrow(() -> new DomainException(EmailVerificationToken.INVALID_LINK));
		if (account.isEmailVerified()) {
			return;
		}
		token.consume(clock.instant());
		account.verifyEmail();
		tokens.save(token);
		accounts.save(account);
	}
}
