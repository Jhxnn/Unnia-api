package com.Unnia.identity.application.port;

import com.Unnia.identity.domain.model.Email;

/** Entrega o e-mail de confirmação. O adaptador monta o link e o texto; aqui só se diz para quem e com qual token. */
public interface VerificationEmailSender {

	/**
	 * @param recipientName nome para a saudação; pode vir vazio
	 * @param token         o token em claro, que vai no link
	 */
	void send(Email to, String recipientName, String token);
}
