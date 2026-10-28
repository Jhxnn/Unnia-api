package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.domain.id.AccountId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaAccountRepository implements AccountRepository {

	private final AccountJpaRepository jpa;

	JpaAccountRepository(AccountJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Account save(Account account) {
		jpa.save(IdentityMapper.toEntity(account));
		return account;
	}

	@Override
	public Optional<Account> findById(AccountId id) {
		return jpa.findById(id.value()).map(IdentityMapper::toDomain);
	}

	@Override
	public Optional<Account> findByEmail(Email email) {
		return jpa.findByEmail(email.value()).map(IdentityMapper::toDomain);
	}

	@Override
	public boolean existsByEmail(Email email) {
		return jpa.existsByEmail(email.value());
	}
}
