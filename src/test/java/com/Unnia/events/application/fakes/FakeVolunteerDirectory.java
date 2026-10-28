package com.Unnia.events.application.fakes;

import com.Unnia.events.application.port.VolunteerDirectory;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeVolunteerDirectory implements VolunteerDirectory {

	private final Map<AccountId, VolunteerId> byAccount = new HashMap<>();

	public VolunteerId add(AccountId accountId) {
		VolunteerId volunteerId = VolunteerId.generate();
		byAccount.put(accountId, volunteerId);
		return volunteerId;
	}

	@Override
	public Optional<VolunteerId> findVolunteerIdByAccount(AccountId accountId) {
		return Optional.ofNullable(byAccount.get(accountId));
	}
}
