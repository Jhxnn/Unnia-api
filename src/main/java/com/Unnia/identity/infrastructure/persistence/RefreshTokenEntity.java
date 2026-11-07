package com.Unnia.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * O {@code version} faz duas renovações simultâneas do mesmo token não terminarem ambas bem-sucedidas:
 * a segunda falha por bloqueio otimista.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity {

	@Id
	UUID id;

	@Column(name = "account_id", nullable = false)
	UUID accountId;

	@Column(name = "family_id", nullable = false)
	UUID familyId;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	String tokenHash;

	@Column(name = "issued_at", nullable = false)
	Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	Instant expiresAt;

	@Column(name = "used_at")
	Instant usedAt;

	@Column(name = "revoked_at")
	Instant revokedAt;

	@Version
	Long version;

	protected RefreshTokenEntity() {
	}
}
