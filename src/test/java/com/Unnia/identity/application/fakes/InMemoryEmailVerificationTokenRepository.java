package com.Unnia.identity.application.fakes;

import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.identity.domain.model.EmailVerificationTokenId;
import com.Unnia.identity.domain.repository.EmailVerificationTokenRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryEmailVerificationTokenRepository implements EmailVerificationTokenRepository {

	private final Map<EmailVerificationTokenId, EmailVerificationToken> store = new LinkedHashMap<>();

	@Override
	public EmailVerificationToken save(EmailVerificationToken token) {
		store.put(token.id(), token);
		return token;
	}

	@Override
	public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
		return store.values().stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
	}

	@Override
	public Optional<EmailVerificationToken> findLatestOf(AccountId accountId) {
		return store.values().stream().filter(t -> t.accountId().equals(accountId))
				.max(Comparator.comparing(EmailVerificationToken::issuedAt));
	}

	@Override
	public void invalidateAllOf(AccountId accountId, Instant now) {
		store.values().stream().filter(t -> t.accountId().equals(accountId)).forEach(t -> t.invalidate(now));
	}

	public List<EmailVerificationToken> all() {
		return List.copyOf(store.values());
	}
}
