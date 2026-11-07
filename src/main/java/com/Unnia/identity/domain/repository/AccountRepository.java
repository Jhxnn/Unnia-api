package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(AccountId id);

	Optional<Account> findByEmail(Email email);

	boolean existsByEmail(Email email);

	/** As contas que existirem entre os ids informados; as demais são ignoradas. */
	List<Account> findAllById(Collection<AccountId> ids);
}
