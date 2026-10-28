package com.Unnia.events.application.fakes;

import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeNgoDirectory implements NgoDirectory {

	private final Map<AccountId, NgoId> byAccount = new HashMap<>();
	private final Set<NgoId> verified = new HashSet<>();

	public NgoId add(AccountId accountId, boolean isVerified) {
		NgoId ngoId = NgoId.generate();
		byAccount.put(accountId, ngoId);
		if (isVerified) {
			verified.add(ngoId);
		}
		return ngoId;
	}

	@Override
	public Optional<NgoId> findNgoIdByAccount(AccountId accountId) {
		return Optional.ofNullable(byAccount.get(accountId));
	}

	@Override
	public boolean isVerified(NgoId ngoId) {
		return verified.contains(ngoId);
	}
}
