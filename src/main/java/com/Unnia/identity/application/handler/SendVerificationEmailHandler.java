package com.Unnia.identity.application.handler;

import com.Unnia.identity.application.EmailVerificationIssuer;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.application.DomainEventHandler;

/**
 * Depois que o cadastro é confirmado no banco, envia o e-mail de confirmação. Se o envio falhar, o cadastro
 * continua valendo e a pessoa pode pedir o reenvio na tela de login.
 */
public class SendVerificationEmailHandler implements DomainEventHandler<AccountRegistered> {

	private final AccountRepository accounts;
	private final EmailVerificationIssuer issuer;

	public SendVerificationEmailHandler(AccountRepository accounts, EmailVerificationIssuer issuer) {
		this.accounts = accounts;
		this.issuer = issuer;
	}

	@Override
	public Class<AccountRegistered> eventType() {
		return AccountRegistered.class;
	}

	@Override
	public void handle(AccountRegistered event) {
		accounts.findById(event.accountId()).ifPresent(issuer::issueFor);
	}
}
