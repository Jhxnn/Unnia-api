package com.Unnia.identity.application.fakes;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {

	private final Map<AccountId, Account> store = new HashMap<>();

	@Override
	public Account save(Account account) {
		store.put(account.id(), account);
		return account;
	}

	@Override
	public Optional<Account> findById(AccountId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<Account> findByEmail(Email email) {
		return store.values().stream().filter(a -> a.email().equals(email)).findFirst();
	}

	@Override
	public boolean existsByEmail(Email email) {
		return findByEmail(email).isPresent();
	}

	@Override
	public List<Account> findAllById(Collection<AccountId> ids) {
		return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
	}

	public int size() {
		return store.size();
	}
}
