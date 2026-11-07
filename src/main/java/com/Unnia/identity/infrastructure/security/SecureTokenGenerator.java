package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.application.port.TokenGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** Token opaco de 256 bits vindos de um gerador criptograficamente seguro, em base64 sem padding. */
@Component
class SecureTokenGenerator implements TokenGenerator {

	private static final int BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	@Override
	public String newToken() {
		byte[] bytes = new byte[BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
