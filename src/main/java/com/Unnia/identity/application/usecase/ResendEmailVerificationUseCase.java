package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.EmailVerificationIssuer;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.domain.DomainException;

/**
 * Reenvia o e-mail de confirmação. Responde sempre igual, exista a conta ou não, para a tela não servir de
 * consulta de quais e-mails estão cadastrados.
 */
public class ResendEmailVerificationUseCase {

	public record Command(String email) {
	}

	private final AccountRepository accounts;
	private final EmailVerificationIssuer issuer;

	public ResendEmailVerificationUseCase(AccountRepository accounts, EmailVerificationIssuer issuer) {
		this.accounts = accounts;
		this.issuer = issuer;
	}

	public void execute(Command command) {
		Email email;
		try {
			email = Email.of(command.email());
		} catch (DomainException e) {
			return;
		}
		accounts.findByEmail(email).filter(account -> account.isActive()).ifPresent(issuer::issueUnlessRecent);
	}
}
