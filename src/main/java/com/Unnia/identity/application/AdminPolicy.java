package com.Unnia.identity.application;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.id.AccountId;

/** Autorização das operações restritas à equipe da plataforma. */
public class AdminPolicy {

	private final AccountRepository accounts;

	public AdminPolicy(AccountRepository accounts) {
		this.accounts = accounts;
	}

	public void requireAdmin(AccountId accountId) {
		Account account = accounts.findById(accountId)
				.orElseThrow(() -> new ForbiddenException("Acesso restrito a administradores"));
		if (!account.hasRole(Role.ADMIN) || !account.isActive()) {
			throw new ForbiddenException("Acesso restrito a administradores");
		}
	}
}
