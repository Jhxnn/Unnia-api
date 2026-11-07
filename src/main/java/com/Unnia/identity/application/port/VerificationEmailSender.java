package com.Unnia.identity.application.port;

import com.Unnia.identity.domain.model.Email;

/** Entrega o e-mail com o código de confirmação. O adaptador monta o texto; aqui só se diz para quem e qual código. */
public interface VerificationEmailSender {

	/** @param recipientName nome para a saudação; pode vir vazio */
	void send(Email to, String recipientName, String code);
}
