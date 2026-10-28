package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaVolunteerProfileRepository implements VolunteerProfileRepository {

	private final VolunteerProfileJpaRepository jpa;

	JpaVolunteerProfileRepository(VolunteerProfileJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public VolunteerProfile save(VolunteerProfile profile) {
		jpa.save(IdentityMapper.toEntity(profile));
		return profile;
	}

	@Override
	public Optional<VolunteerProfile> findById(VolunteerId id) {
		return jpa.findById(id.value()).map(IdentityMapper::toDomain);
	}

	@Override
	public Optional<VolunteerProfile> findByAccountId(AccountId accountId) {
		return jpa.findByAccountId(accountId.value()).map(IdentityMapper::toDomain);
	}
}
