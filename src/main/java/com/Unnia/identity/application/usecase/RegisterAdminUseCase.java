package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.RawPassword;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.id.AccountId;

/**
 * Criação do administrador da plataforma, chamada por seed/configuração na inicialização.
 * Idempotente: se o admin já existe, não faz nada.
 */
public class RegisterAdminUseCase {

	public record Command(String email, String password) {
	}

	public record Result(AccountId accountId, boolean created) {
	}

	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;
	private final DomainEventPublisher events;

	public RegisterAdminUseCase(AccountRepository accounts, PasswordEncoder passwordEncoder,
			DomainEventPublisher events) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
		this.events = events;
	}

	public Result execute(Command command) {
		Email email = Email.of(command.email());

		var existing = accounts.findByEmail(email);
		if (existing.isPresent()) {
			if (!existing.get().hasRole(Role.ADMIN)) {
				throw new ConflictException("E-mail já cadastrado com outro papel");
			}
			return new Result(existing.get().id(), false);
		}

		Account admin = Account.register(email, passwordEncoder.encode(RawPassword.of(command.password())),
				Role.ADMIN);
		accounts.save(admin);
		events.publishFrom(admin);
		return new Result(admin.id(), true);
	}
}
