package com.Unnia.identity.application;

import com.Unnia.identity.application.port.CodeGenerator;
import com.Unnia.identity.application.port.TokenHasher;
import com.Unnia.identity.application.port.VerificationEmailSender;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.identity.domain.repository.EmailVerificationTokenRepository;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Emite um código novo e o envia por e-mail. Só um código vale por vez: ao emitir, os anteriores são
 * inutilizados. Contas já confirmadas não recebem nada.
 */
public class EmailVerificationIssuer {

	public static final Duration TTL = Duration.ofMinutes(15);
	/** Intervalo mínimo entre dois envios pedidos pelo usuário, para a tela de reenvio não virar spam. */
	public static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

	private final EmailVerificationTokenRepository tokens;
	private final CodeGenerator codes;
	private final TokenHasher hasher;
	private final VerificationEmailSender sender;
	private final NgoProfileRepository ngos;
	private final VolunteerProfileRepository volunteers;
	private final Clock clock;

	public EmailVerificationIssuer(EmailVerificationTokenRepository tokens, CodeGenerator codes, TokenHasher hasher,
			VerificationEmailSender sender, NgoProfileRepository ngos, VolunteerProfileRepository volunteers,
			Clock clock) {
		this.tokens = tokens;
		this.codes = codes;
		this.hasher = hasher;
		this.sender = sender;
		this.ngos = ngos;
		this.volunteers = volunteers;
		this.clock = clock;
	}

	/** @return verdadeiro se um e-mail foi enviado */
	public boolean issueFor(Account account) {
		if (account.isEmailVerified()) {
			return false;
		}
		Instant now = clock.instant();
		tokens.invalidateAllOf(account.id(), now);
		String code = codes.newCode();
		tokens.save(EmailVerificationToken.issue(account.id(),
				hasher.hash(EmailVerificationToken.secretOf(account.id(), code)), now, TTL));
		sender.send(account.email(), nameOf(account), code);
		return true;
	}

	/** Como {@link #issueFor}, mas ignora o pedido se um código foi enviado há pouco. */
	public boolean issueUnlessRecent(Account account) {
		Instant now = clock.instant();
		boolean recent = tokens.findLatestOf(account.id())
				.filter(latest -> latest.issuedAt().plus(RESEND_COOLDOWN).isAfter(now)).isPresent();
		return !recent && issueFor(account);
	}

	private String nameOf(Account account) {
		return ngos.findByAccountId(account.id()).map(profile -> profile.name().value())
				.or(() -> volunteers.findByAccountId(account.id()).map(profile -> profile.name().value()))
				.orElse("");
	}
}
