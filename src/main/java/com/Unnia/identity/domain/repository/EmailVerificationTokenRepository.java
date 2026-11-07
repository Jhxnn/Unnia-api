package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository {

	EmailVerificationToken save(EmailVerificationToken token);

	/** O código mais recente enviado para a conta, usado ou não. */
	Optional<EmailVerificationToken> findLatestOf(AccountId accountId);

	/** Inutiliza os códigos ainda pendentes da conta, para que só o mais novo valha. */
	void invalidateAllOf(AccountId accountId, Instant now);
}
