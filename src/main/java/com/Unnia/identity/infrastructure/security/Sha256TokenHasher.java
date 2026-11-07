package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.application.port.TokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 em hexadecimal (64 caracteres). Basta um hash rápido: o token tem 256 bits de entropia,
 * então não é adivinhável por força bruta como uma senha.
 */
@Component
class Sha256TokenHasher implements TokenHasher {

	@Override
	public String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponível", e);
		}
	}
}
