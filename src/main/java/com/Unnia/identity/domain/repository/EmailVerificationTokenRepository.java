package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository {

	EmailVerificationToken save(EmailVerificationToken token);

	Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

	/** O link mais recente enviado para a conta, usado ou não. */
	Optional<EmailVerificationToken> findLatestOf(AccountId accountId);

	/** Inutiliza os links ainda pendentes da conta, para que só o mais novo valha. */
	void invalidateAllOf(AccountId accountId, Instant now);
}
