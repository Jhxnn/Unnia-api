package com.Unnia.identity.application.fakes;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryVolunteerProfileRepository implements VolunteerProfileRepository {

	private final Map<VolunteerId, VolunteerProfile> store = new HashMap<>();

	@Override
	public VolunteerProfile save(VolunteerProfile profile) {
		store.put(profile.id(), profile);
		return profile;
	}

	@Override
	public Optional<VolunteerProfile> findById(VolunteerId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<VolunteerProfile> findByAccountId(AccountId accountId) {
		return store.values().stream().filter(p -> p.accountId().equals(accountId)).findFirst();
	}

	public int size() {
		return store.size();
	}
}
