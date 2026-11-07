package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

	RefreshToken save(RefreshToken token);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Revoga todos os tokens ainda ativos da família (uma sessão). */
	void revokeFamily(UUID familyId, Instant now);

	/** Revoga todas as sessões da conta, por exemplo após a troca de senha. */
	void revokeAllOf(AccountId accountId, Instant now);
}
