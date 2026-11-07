package com.Unnia.identity.application.fakes;

import com.Unnia.identity.application.port.VerificationEmailSender;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.shared.application.ServiceUnavailableException;

import java.util.ArrayList;
import java.util.List;

/** Guarda o que seria enviado, e pode simular o servidor de e-mail fora do ar. */
public class RecordingVerificationEmailSender implements VerificationEmailSender {

	public record Sent(Email to, String recipientName, String token) {
	}

	private final List<Sent> sent = new ArrayList<>();
	private boolean down;

	public void goDown() {
		down = true;
	}

	@Override
	public void send(Email to, String recipientName, String token) {
		if (down) {
			throw new ServiceUnavailableException("fora do ar");
		}
		sent.add(new Sent(to, recipientName, token));
	}

	public List<Sent> sent() {
		return List.copyOf(sent);
	}
}
