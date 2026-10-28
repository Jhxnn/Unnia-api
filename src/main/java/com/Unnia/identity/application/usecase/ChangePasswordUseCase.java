package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.RawPassword;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.id.AccountId;

public class ChangePasswordUseCase {

	public record Command(AccountId accountId, String currentPassword, String newPassword) {
	}

	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;

	public ChangePasswordUseCase(AccountRepository accounts, PasswordEncoder passwordEncoder) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
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
	}
}
