package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Credencial de longa duração que renova o token de acesso. Guarda apenas o hash do token entregue
 * ao cliente. Cada uso gera um sucessor na mesma <em>família</em> (rotação); reapresentar um token
 * já usado indica roubo e deve derrubar a família inteira.
 */
public class RefreshToken extends AggregateRoot<RefreshTokenId> {

	private final AccountId accountId;
	private final UUID familyId;
	private final String tokenHash;
	private final Instant issuedAt;
	private final Instant expiresAt;
	private Instant usedAt;
	private Instant revokedAt;

	private RefreshToken(RefreshTokenId id, AccountId accountId, UUID familyId, String tokenHash, Instant issuedAt,
			Instant expiresAt, Instant usedAt, Instant revokedAt) {
		super(id);
		this.accountId = Guard.notNull(accountId, "Conta é obrigatória");
		this.familyId = Guard.notNull(familyId, "Família é obrigatória");
		this.tokenHash = Guard.notBlank(tokenHash, "Hash do token é obrigatório");
		this.issuedAt = Guard.notNull(issuedAt, "Data de emissão é obrigatória");
		this.expiresAt = Guard.notNull(expiresAt, "Data de expiração é obrigatória");
		this.usedAt = usedAt;
		this.revokedAt = revokedAt;
	}

	/** Primeiro token de uma nova sessão (login): abre uma família nova. */
	public static RefreshToken startSession(AccountId accountId, String tokenHash, Instant now, Duration ttl) {
		return new RefreshToken(RefreshTokenId.generate(), accountId, UUID.randomUUID(), tokenHash, now,
				now.plus(ttl), null, null);
	}

	public static RefreshToken restore(RefreshTokenId id, AccountId accountId, UUID familyId, String tokenHash,
			Instant issuedAt, Instant expiresAt, Instant usedAt, Instant revokedAt) {
		return new RefreshToken(id, accountId, familyId, tokenHash, issuedAt, expiresAt, usedAt, revokedAt);
	}

	/** Consome este token e devolve o sucessor, da mesma família e com novo prazo. */
	public RefreshToken rotate(String newTokenHash, Instant now, Duration ttl) {
		Guard.check(isUsable(now), "Token de renovação inválido");
		this.usedAt = now;
		return new RefreshToken(RefreshTokenId.generate(), accountId, familyId, newTokenHash, now, now.plus(ttl),
				null, null);
	}

	/** Idempotente. */
	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && revokedAt == null && now.isBefore(expiresAt);
	}

	/** Verdadeiro se o token já foi trocado por um sucessor; apresentá-lo de novo é sinal de reuso. */
	public boolean wasUsed() {
		return usedAt != null;
	}

	public AccountId accountId() {
		return accountId;
	}

	public UUID familyId() {
		return familyId;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public Instant issuedAt() {
		return issuedAt;
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	public Optional<Instant> usedAt() {
		return Optional.ofNullable(usedAt);
	}

	public Optional<Instant> revokedAt() {
		return Optional.ofNullable(revokedAt);
	}
}
