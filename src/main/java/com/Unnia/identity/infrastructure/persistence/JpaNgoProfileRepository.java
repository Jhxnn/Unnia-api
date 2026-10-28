package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaNgoProfileRepository implements NgoProfileRepository {

	private final NgoProfileJpaRepository jpa;

	JpaNgoProfileRepository(NgoProfileJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public NgoProfile save(NgoProfile profile) {
		jpa.save(IdentityMapper.toEntity(profile));
		return profile;
	}

	@Override
	public Optional<NgoProfile> findById(NgoId id) {
		return jpa.findById(id.value()).map(IdentityMapper::toDomain);
	}

	@Override
	public Optional<NgoProfile> findByAccountId(AccountId accountId) {
		return jpa.findByAccountId(accountId.value()).map(IdentityMapper::toDomain);
	}

	@Override
	public boolean existsByCnpj(Cnpj cnpj) {
		return jpa.existsByCnpj(cnpj.value());
	}
}
