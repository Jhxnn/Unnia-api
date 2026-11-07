package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.identity.domain.repository.RefreshTokenRepository;
import com.Unnia.shared.domain.id.AccountId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

	private final RefreshTokenJpaRepository jpa;

	JpaRefreshTokenRepository(RefreshTokenJpaRepository jpa) {
		this.jpa = jpa;
	}

	/** Reaproveita a entidade carregada (e sua versão) para o bloqueio otimista funcionar. */
	@Override
	@Transactional
	public RefreshToken save(RefreshToken token) {
		RefreshTokenEntity entity = jpa.findById(token.id().value()).orElseGet(RefreshTokenEntity::new);
		RefreshTokenMapper.apply(token, entity);
		jpa.save(entity);
		return token;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return jpa.findByTokenHash(tokenHash).map(RefreshTokenMapper::toDomain);
	}

	@Override
	@Transactional
	public void revokeFamily(UUID familyId, Instant now) {
		jpa.revokeFamily(familyId, now);
	}

	@Override
	@Transactional
	public void revokeAllOf(AccountId accountId, Instant now) {
		jpa.revokeAllOf(accountId.value(), now);
	}
}
