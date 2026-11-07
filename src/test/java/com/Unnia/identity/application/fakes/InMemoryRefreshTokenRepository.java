package com.Unnia.identity.application.fakes;

import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.identity.domain.model.RefreshTokenId;
import com.Unnia.identity.domain.repository.RefreshTokenRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

	private final Map<RefreshTokenId, RefreshToken> store = new LinkedHashMap<>();

	@Override
	public RefreshToken save(RefreshToken token) {
		store.put(token.id(), token);
		return token;
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return store.values().stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
	}

	@Override
	public void revokeFamily(UUID familyId, Instant now) {
		store.values().stream().filter(t -> t.familyId().equals(familyId)).forEach(t -> t.revoke(now));
	}

	@Override
	public void revokeAllOf(AccountId accountId, Instant now) {
		store.values().stream().filter(t -> t.accountId().equals(accountId)).forEach(t -> t.revoke(now));
	}

	public List<RefreshToken> all() {
		return List.copyOf(store.values());
	}
}
