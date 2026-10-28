package com.Unnia.social.application.fakes;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.port.AccountDirectory;
import com.Unnia.social.application.port.EventReferences;

import java.util.HashSet;
import java.util.Set;

public class FakeDirectories implements AccountDirectory, EventReferences {

	private final Set<AccountId> activeAccounts = new HashSet<>();
	private final Set<EventId> events = new HashSet<>();

	public AccountId newAccount() {
		AccountId id = AccountId.generate();
		activeAccounts.add(id);
		return id;
	}

	public EventId newEvent() {
		EventId id = EventId.generate();
		events.add(id);
		return id;
	}

	@Override
	public boolean isActive(AccountId accountId) {
		return activeAccounts.contains(accountId);
	}

	@Override
	public boolean exists(EventId eventId) {
		return events.contains(eventId);
	}
}
