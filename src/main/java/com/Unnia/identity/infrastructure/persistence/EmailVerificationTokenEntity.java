package com.Unnia.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationTokenEntity {

	@Id
	UUID id;

	@Column(name = "account_id", nullable = false)
	UUID accountId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	String tokenHash;

	@Column(name = "issued_at", nullable = false)
	Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	Instant expiresAt;

	@Column(name = "used_at")
	Instant usedAt;

	@Column(name = "failed_attempts", nullable = false)
	int failedAttempts;

	protected EmailVerificationTokenEntity() {
	}
}
