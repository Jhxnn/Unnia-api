package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.Optional;

public interface VolunteerProfileRepository {

	VolunteerProfile save(VolunteerProfile profile);

	Optional<VolunteerProfile> findById(VolunteerId id);

	Optional<VolunteerProfile> findByAccountId(AccountId accountId);
}
