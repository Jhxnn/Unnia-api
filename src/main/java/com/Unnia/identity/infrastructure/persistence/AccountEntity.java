package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.AccountStatus;
import com.Unnia.identity.domain.model.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "accounts")
class AccountEntity {

	@Id
	UUID id;

	@Column(nullable = false, unique = true, length = 254)
	String email;

	@Column(name = "password_hash", nullable = false)
	String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	AccountStatus status;

	protected AccountEntity() {
	}
}
