package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Link de uso único enviado por e-mail para provar que a caixa de entrada existe. Guarda apenas o hash do
 * token que foi para o e-mail; quem abre o link apresenta o token, e o hash é recalculado para achar este registro.
 */
public class EmailVerificationToken extends AggregateRoot<EmailVerificationTokenId> {

	public static final String INVALID_LINK = "Link de confirmação inválido ou expirado";

	private final AccountId accountId;
	private final String tokenHash;
	private final Instant issuedAt;
	private final Instant expiresAt;
	private Instant usedAt;

	private EmailVerificationToken(EmailVerificationTokenId id, AccountId accountId, String tokenHash,
			Instant issuedAt, Instant expiresAt, Instant usedAt) {
		super(id);
		this.accountId = Guard.notNull(accountId, "Conta é obrigatória");
		this.tokenHash = Guard.notBlank(tokenHash, "Hash do token é obrigatório");
		this.issuedAt = Guard.notNull(issuedAt, "Data de emissão é obrigatória");
		this.expiresAt = Guard.notNull(expiresAt, "Data de expiração é obrigatória");
		this.usedAt = usedAt;
	}

	public static EmailVerificationToken issue(AccountId accountId, String tokenHash, Instant now, Duration ttl) {
		return new EmailVerificationToken(EmailVerificationTokenId.generate(), accountId, tokenHash, now,
				now.plus(ttl), null);
	}

	public static EmailVerificationToken restore(EmailVerificationTokenId id, AccountId accountId, String tokenHash,
			Instant issuedAt, Instant expiresAt, Instant usedAt) {
		return new EmailVerificationToken(id, accountId, tokenHash, issuedAt, expiresAt, usedAt);
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && now.isBefore(expiresAt);
	}

	/** Gasta o link. Usar duas vezes, ou depois de vencido, é recusado. */
	public void consume(Instant now) {
		Guard.check(isUsable(now), INVALID_LINK);
		usedAt = now;
	}

	/** Inutiliza o link (por exemplo, quando um novo é enviado). Idempotente. */
	public void invalidate(Instant now) {
		if (usedAt == null) {
			usedAt = now;
		}
	}

	public AccountId accountId() {
		return accountId;
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
}
