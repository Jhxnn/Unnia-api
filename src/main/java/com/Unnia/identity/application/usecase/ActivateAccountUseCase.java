package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.AdminPolicy;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

public class ActivateAccountUseCase {

	public record Command(AccountId adminId, AccountId targetId) {
	}

	private final AdminPolicy adminPolicy;
	private final AccountRepository accounts;
	private final DomainEventPublisher events;

	public ActivateAccountUseCase(AdminPolicy adminPolicy, AccountRepository accounts,
			DomainEventPublisher events) {
		this.adminPolicy = adminPolicy;
		this.accounts = accounts;
		this.events = events;
	}

	public void execute(Command command) {
		adminPolicy.requireAdmin(command.adminId());
		Account target = accounts.findById(command.targetId())
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));

		target.activate();

		accounts.save(target);
		events.publishFrom(target);
	}
}
