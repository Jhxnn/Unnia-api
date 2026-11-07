package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.RefreshToken;
import com.Unnia.identity.domain.model.RefreshTokenId;
import com.Unnia.shared.domain.id.AccountId;

final class RefreshTokenMapper {

	private RefreshTokenMapper() {
	}

	/** Copia o estado para a entidade informada, preservando a versão do bloqueio otimista. */
	static void apply(RefreshToken token, RefreshTokenEntity entity) {
		entity.id = token.id().value();
		entity.accountId = token.accountId().value();
		entity.familyId = token.familyId();
		entity.tokenHash = token.tokenHash();
		entity.issuedAt = token.issuedAt();
		entity.expiresAt = token.expiresAt();
		entity.usedAt = token.usedAt().orElse(null);
		entity.revokedAt = token.revokedAt().orElse(null);
	}

	static RefreshToken toDomain(RefreshTokenEntity entity) {
		return RefreshToken.restore(new RefreshTokenId(entity.id), new AccountId(entity.accountId), entity.familyId,
				entity.tokenHash, entity.issuedAt, entity.expiresAt, entity.usedAt, entity.revokedAt);
	}
}
