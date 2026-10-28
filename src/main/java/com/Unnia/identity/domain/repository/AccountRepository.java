package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Optional;

public interface AccountRepository {

	Account save(Account account);

	Optional<Account> findById(AccountId id);

	Optional<Account> findByEmail(Email email);

	boolean existsByEmail(Email email);
}
