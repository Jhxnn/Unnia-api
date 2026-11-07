package com.Unnia.social.application.fakes;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.port.AccountDirectory;
import com.Unnia.social.application.port.AuthorDirectory;
import com.Unnia.social.application.port.EventReferences;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FakeDirectories implements AccountDirectory, EventReferences, AuthorDirectory {

	private final Set<AccountId> activeAccounts = new HashSet<>();
	private final Set<EventId> events = new HashSet<>();
	private final Map<AccountId, Author> authors = new HashMap<>();

	public AccountId newAccount() {
		AccountId id = AccountId.generate();
		activeAccounts.add(id);
		return id;
	}

	/** Conta ativa com nome e papel conhecidos pelo diretório de autores. */
	public AccountId newAccount(String name, String role) {
		AccountId id = newAccount();
		authors.put(id, new Author(name, role, UUID.randomUUID()));
		return id;
	}

	@Override
	public Map<AccountId, Author> authorsOf(Collection<AccountId> accountIds) {
		Map<AccountId, Author> result = new HashMap<>();
		for (AccountId id : accountIds) {
			if (authors.containsKey(id)) {
				result.put(id, authors.get(id));
			}
		}
		return result;
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
