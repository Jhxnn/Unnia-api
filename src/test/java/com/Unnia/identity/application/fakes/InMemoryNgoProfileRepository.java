package com.Unnia.identity.application.fakes;

import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryNgoProfileRepository implements NgoProfileRepository {

	private final Map<NgoId, NgoProfile> store = new HashMap<>();

	@Override
	public NgoProfile save(NgoProfile profile) {
		store.put(profile.id(), profile);
		return profile;
	}

	@Override
	public Optional<NgoProfile> findById(NgoId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<NgoProfile> findByAccountId(AccountId accountId) {
		return store.values().stream().filter(p -> p.accountId().equals(accountId)).findFirst();
	}

	@Override
	public boolean existsByCnpj(Cnpj cnpj) {
		return store.values().stream().anyMatch(p -> p.cnpj().equals(cnpj));
	}

	public int size() {
		return store.size();
	}
}
