package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.AccountActivated;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.event.AccountSuspended;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

public class Account extends AggregateRoot<AccountId> {

	private final Email email;
	private final Role role;
	private PasswordHash passwordHash;
	private AccountStatus status;
	private boolean emailVerified;

	private Account(AccountId id, Email email, PasswordHash passwordHash, Role role, AccountStatus status,
			boolean emailVerified) {
		super(id);
		this.email = Guard.notNull(email, "E-mail é obrigatório");
		this.passwordHash = Guard.notNull(passwordHash, "Senha é obrigatória");
		this.role = Guard.notNull(role, "Papel é obrigatório");
		this.status = Guard.notNull(status, "Status é obrigatório");
		this.emailVerified = emailVerified;
	}

	public static Account register(Email email, PasswordHash passwordHash, Role role) {
		// o administrador é criado pela plataforma; as demais contas precisam provar que o e-mail existe
		Account account = new Account(AccountId.generate(), email, passwordHash, role, AccountStatus.ACTIVE,
				role == Role.ADMIN);
		account.raise(new AccountRegistered(account.id(), role));
		return account;
	}

	public static Account restore(AccountId id, Email email, PasswordHash passwordHash, Role role,
			AccountStatus status, boolean emailVerified) {
		return new Account(id, email, passwordHash, role, status, emailVerified);
	}

	public void suspend() {
		Guard.check(isActive(), "Conta já está suspensa");
		status = AccountStatus.SUSPENDED;
		raise(new AccountSuspended(id()));
	}

	public void activate() {
		Guard.check(!isActive(), "Conta já está ativa");
		status = AccountStatus.ACTIVE;
		raise(new AccountActivated(id()));
	}

	public void changePassword(PasswordHash newPasswordHash) {
		this.passwordHash = Guard.notNull(newPasswordHash, "Senha é obrigatória");
	}

	/** Idempotente: confirmar de novo não muda nada. */
	public void verifyEmail() {
		emailVerified = true;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public boolean isActive() {
		return status == AccountStatus.ACTIVE;
	}

	public boolean hasRole(Role expected) {
		return role == expected;
	}

	public Email email() {
		return email;
	}

	public PasswordHash passwordHash() {
		return passwordHash;
	}

	public Role role() {
		return role;
	}

	public AccountStatus status() {
		return status;
	}
}
