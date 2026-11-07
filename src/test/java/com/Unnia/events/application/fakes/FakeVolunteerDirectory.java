package com.Unnia.events.application.fakes;

import com.Unnia.events.application.port.VolunteerDirectory;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeVolunteerDirectory implements VolunteerDirectory {

	private final Map<AccountId, VolunteerId> byAccount = new HashMap<>();
	private final Map<VolunteerId, VolunteerSummary> summaries = new HashMap<>();

	public VolunteerId add(AccountId accountId) {
		return add(accountId, "Voluntário", "Recife");
	}

	public VolunteerId add(AccountId accountId, String name, String city) {
		VolunteerId volunteerId = VolunteerId.generate();
		byAccount.put(accountId, volunteerId);
		summaries.put(volunteerId, new VolunteerSummary(name, city));
		return volunteerId;
	}

	/** Simula um voluntário cujo perfil deixou de existir. */
	public void forget(VolunteerId volunteerId) {
		summaries.remove(volunteerId);
	}

	@Override
	public Optional<VolunteerId> findVolunteerIdByAccount(AccountId accountId) {
		return Optional.ofNullable(byAccount.get(accountId));
	}

	@Override
	public Map<VolunteerId, VolunteerSummary> summariesOf(Collection<VolunteerId> volunteerIds) {
		Map<VolunteerId, VolunteerSummary> found = new HashMap<>();
		volunteerIds.forEach(id -> {
			if (summaries.containsKey(id)) {
				found.put(id, summaries.get(id));
			}
		});
		return found;
	}
}
