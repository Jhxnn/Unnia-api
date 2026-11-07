package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.RawPassword;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.RefreshTokenRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;

/** Troca a senha e encerra todas as sessões da conta, para que um token vazado deixe de valer. */
public class ChangePasswordUseCase {

	public record Command(AccountId accountId, String currentPassword, String newPassword) {
	}

	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokens;
	private final Clock clock;

	public ChangePasswordUseCase(AccountRepository accounts, PasswordEncoder passwordEncoder,
			RefreshTokenRepository refreshTokens, Clock clock) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
	}

	public void execute(Command command) {
		Account account = accounts.findById(command.accountId())
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));
		if (command.currentPassword() == null
				|| !passwordEncoder.matches(command.currentPassword(), account.passwordHash())) {
			throw new UnauthorizedException("Senha atual incorreta");
		}

		account.changePassword(passwordEncoder.encode(RawPassword.of(command.newPassword())));
		accounts.save(account);
		refreshTokens.revokeAllOf(account.id(), clock.instant());
	}
}
