package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.TokenHasher;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.EmailVerificationTokenRepository;
import com.Unnia.shared.domain.DomainException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Confirma o e-mail com o código de 6 dígitos que a pessoa recebeu.
 *
 * <p>Devolve o resultado em vez de lançar exceção quando o código não serve: a tentativa errada precisa ser
 * gravada (são só cinco por código), e uma exceção desfaria a transação junto. Quem chama traduz
 * {@link Result#REJECTED} em erro. Conta inexistente, código errado, vencido ou esgotado dão a mesma resposta.
 */
public class VerifyEmailUseCase {

	public record Command(String email, String code) {
	}

	public enum Result {
		CONFIRMED, REJECTED
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

	public Result execute(Command command) {
		String code = command.code() == null ? "" : command.code().trim();
		Optional<Account> found = findAccount(command.email());
		if (code.isEmpty() || found.isEmpty()) {
			return Result.REJECTED;
		}
		Account account = found.get();
		Optional<EmailVerificationToken> latest = tokens.findLatestOf(account.id());
		if (latest.isEmpty()) {
			return Result.REJECTED;
		}
		EmailVerificationToken token = latest.get();
		boolean matches = matches(token, account, code);

		if (account.isEmailVerified()) {
			// repetir o código que já confirmou a conta não é erro; qualquer outro valor não revela nada
			return matches ? Result.CONFIRMED : Result.REJECTED;
		}
		Instant now = clock.instant();
		if (!token.isUsable(now)) {
			return Result.REJECTED;
		}
		if (!matches) {
			token.recordFailure(now);
			tokens.save(token);
			return Result.REJECTED;
		}
		token.consume(now);
		account.verifyEmail();
		tokens.save(token);
		accounts.save(account);
		return Result.CONFIRMED;
	}

	private Optional<Account> findAccount(String rawEmail) {
		try {
			return accounts.findByEmail(Email.of(rawEmail)).filter(Account::isActive);
		} catch (DomainException e) {
			return Optional.empty();
		}
	}

	private boolean matches(EmailVerificationToken token, Account account, String code) {
		byte[] expected = token.tokenHash().getBytes(StandardCharsets.UTF_8);
		byte[] given = hasher.hash(EmailVerificationToken.secretOf(account.id(), code)).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, given);
	}
}
