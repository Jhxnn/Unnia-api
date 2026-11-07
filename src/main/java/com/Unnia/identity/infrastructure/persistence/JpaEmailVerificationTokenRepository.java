package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.EmailVerificationToken;
import com.Unnia.identity.domain.model.EmailVerificationTokenId;
import com.Unnia.identity.domain.repository.EmailVerificationTokenRepository;
import com.Unnia.shared.domain.id.AccountId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
class JpaEmailVerificationTokenRepository implements EmailVerificationTokenRepository {

	private final EmailVerificationTokenJpaRepository jpa;

	JpaEmailVerificationTokenRepository(EmailVerificationTokenJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional
	public EmailVerificationToken save(EmailVerificationToken token) {
		EmailVerificationTokenEntity entity = jpa.findById(token.id().value())
				.orElseGet(EmailVerificationTokenEntity::new);
		entity.id = token.id().value();
		entity.accountId = token.accountId().value();
		entity.tokenHash = token.tokenHash();
		entity.issuedAt = token.issuedAt();
		entity.expiresAt = token.expiresAt();
		entity.usedAt = token.usedAt().orElse(null);
		jpa.save(entity);
		return token;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
		return jpa.findByTokenHash(tokenHash).map(JpaEmailVerificationTokenRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<EmailVerificationToken> findLatestOf(AccountId accountId) {
		return jpa.findFirstByAccountIdOrderByIssuedAtDesc(accountId.value())
				.map(JpaEmailVerificationTokenRepository::toDomain);
	}

	@Override
	@Transactional
	public void invalidateAllOf(AccountId accountId, Instant now) {
		jpa.invalidateAllOf(accountId.value(), now);
	}

	private static EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
		return EmailVerificationToken.restore(new EmailVerificationTokenId(entity.id),
				new AccountId(entity.accountId), entity.tokenHash, entity.issuedAt, entity.expiresAt, entity.usedAt);
	}
}
