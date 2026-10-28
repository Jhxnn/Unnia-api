package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.domain.id.AccountId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emite e valida os tokens JWT (HS256) usados para autenticar a API sem estado. */
@Component
public class JwtService {

	private static final int MIN_SECRET_BYTES = 32;

	private final SecretKey key;
	private final Duration expiration;
	private final Clock clock;

	JwtService(@Value("${unnia.jwt.secret:}") String secret,
			@Value("${unnia.jwt.expiration:PT2H}") Duration expiration, Clock clock) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"unnia.jwt.secret (UNNIA_JWT_SECRET) é obrigatório e deve ter pelo menos 32 caracteres");
		}
		this.key = Keys.hmacShaKeyFor(bytes);
		this.expiration = expiration;
		this.clock = clock;
	}

	public IssuedToken issue(AccountId accountId, Role role) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(expiration);
		String token = Jwts.builder().subject(accountId.value().toString()).claim("role", role.name())
				.issuedAt(Date.from(now)).expiration(Date.from(expiresAt)).signWith(key).compact();
		return new IssuedToken(token, expiresAt);
	}

	/** Vazio se o token for inválido, adulterado ou expirado. */
	public Optional<AuthenticatedUser> parse(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
					.parseSignedClaims(token).getPayload();
			return Optional.of(new AuthenticatedUser(AccountId.of(claims.getSubject()),
					Role.valueOf(claims.get("role", String.class))));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public record IssuedToken(String value, Instant expiresAt) {
	}
}
