package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
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

	@Override
	@Transactional(readOnly = true)
	public List<VolunteerProfile> findAllById(Collection<VolunteerId> ids) {
		return jpa.findAllById(ids.stream().map(VolunteerId::value).toList()).stream()
				.map(IdentityMapper::toDomain).toList();
	}
}
