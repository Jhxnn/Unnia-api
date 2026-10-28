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

	private Account(AccountId id, Email email, PasswordHash passwordHash, Role role, AccountStatus status) {
		super(id);
		this.email = Guard.notNull(email, "E-mail é obrigatório");
		this.passwordHash = Guard.notNull(passwordHash, "Senha é obrigatória");
		this.role = Guard.notNull(role, "Papel é obrigatório");
		this.status = Guard.notNull(status, "Status é obrigatório");
	}

	public static Account register(Email email, PasswordHash passwordHash, Role role) {
		Account account = new Account(AccountId.generate(), email, passwordHash, role, AccountStatus.ACTIVE);
		account.raise(new AccountRegistered(account.id(), role));
		return account;
	}

	public static Account restore(AccountId id, Email email, PasswordHash passwordHash, Role role,
			AccountStatus status) {
		return new Account(id, email, passwordHash, role, status);
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
