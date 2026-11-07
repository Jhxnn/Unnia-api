package com.Unnia.identity.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Código de 6 dígitos enviado por e-mail para provar que a caixa de entrada existe. Guarda só o hash do código.
 * Como 6 dígitos são fáceis de adivinhar, o código vale por pouco tempo e é inutilizado depois de
 * {@value #MAX_FAILED_ATTEMPTS} tentativas erradas.
 */
public class EmailVerificationToken extends AggregateRoot<EmailVerificationTokenId> {

	public static final String INVALID_CODE = "Código inválido ou expirado";
	public static final int MAX_FAILED_ATTEMPTS = 5;

	private final AccountId accountId;
	private final String tokenHash;
	private final Instant issuedAt;
	private final Instant expiresAt;
	private Instant usedAt;
	private int failedAttempts;

	private EmailVerificationToken(EmailVerificationTokenId id, AccountId accountId, String tokenHash,
			Instant issuedAt, Instant expiresAt, Instant usedAt, int failedAttempts) {
		super(id);
		this.accountId = Guard.notNull(accountId, "Conta é obrigatória");
		this.tokenHash = Guard.notBlank(tokenHash, "Hash do código é obrigatório");
		this.issuedAt = Guard.notNull(issuedAt, "Data de emissão é obrigatória");
		this.expiresAt = Guard.notNull(expiresAt, "Data de expiração é obrigatória");
		this.usedAt = usedAt;
		this.failedAttempts = failedAttempts;
	}

	public static EmailVerificationToken issue(AccountId accountId, String tokenHash, Instant now, Duration ttl) {
		return new EmailVerificationToken(EmailVerificationTokenId.generate(), accountId, tokenHash, now,
				now.plus(ttl), null, 0);
	}

	public static EmailVerificationToken restore(EmailVerificationTokenId id, AccountId accountId, String tokenHash,
			Instant issuedAt, Instant expiresAt, Instant usedAt, int failedAttempts) {
		return new EmailVerificationToken(id, accountId, tokenHash, issuedAt, expiresAt, usedAt, failedAttempts);
	}

	/**
	 * O que vai para o hash: o código junto da conta, para que o mesmo código em duas contas não gere o mesmo
	 * registro e para que um vazamento do banco não sirva de tabela pronta.
	 */
	public static String secretOf(AccountId accountId, String code) {
		return accountId.value() + ":" + code;
	}

	public boolean isUsable(Instant now) {
		return usedAt == null && now.isBefore(expiresAt);
	}

	/** Gasta o código. Usar duas vezes, ou depois de vencido, é recusado. */
	public void consume(Instant now) {
		Guard.check(isUsable(now), INVALID_CODE);
		usedAt = now;
	}

	/** Registra uma tentativa errada; ao chegar no limite, o código deixa de valer. */
	public void recordFailure(Instant now) {
		if (usedAt != null) {
			return;
		}
		failedAttempts++;
		if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
			invalidate(now);
		}
	}

	/** Inutiliza o código (por exemplo, quando um novo é enviado). Idempotente. */
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

	public int failedAttempts() {
		return failedAttempts;
	}
}
