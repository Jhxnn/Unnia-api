package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.AccountActivated;
import com.Unnia.identity.domain.event.AccountRegistered;
import com.Unnia.identity.domain.event.AccountSuspended;
import com.Unnia.shared.domain.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

	private static final Email EMAIL = Email.of("ong@exemplo.com");
	private static final PasswordHash HASH = PasswordHash.of("hash");

	@Test
	void registersActiveAccountAndRaisesEvent() {
		Account account = Account.register(EMAIL, HASH, Role.NGO);

		assertThat(account.id()).isNotNull();
		assertThat(account.email()).isEqualTo(EMAIL);
		assertThat(account.role()).isEqualTo(Role.NGO);
		assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.pullDomainEvents()).containsExactly(new AccountRegistered(account.id(), Role.NGO));
	}

	@Test
	void rejectsMissingData() {
		assertThatThrownBy(() -> Account.register(null, HASH, Role.NGO)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Account.register(EMAIL, null, Role.NGO)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Account.register(EMAIL, HASH, null)).isInstanceOf(DomainException.class);
	}

	@Test
	void suspendsAndReactivates() {
		Account account = Account.register(EMAIL, HASH, Role.VOLUNTEER);
		account.pullDomainEvents();

		account.suspend();
		assertThat(account.isActive()).isFalse();
		account.activate();
		assertThat(account.isActive()).isTrue();

		assertThat(account.pullDomainEvents())
				.containsExactly(new AccountSuspended(account.id()), new AccountActivated(account.id()));
	}

	@Test
	void cannotSuspendTwiceOrActivateActiveAccount() {
		Account account = Account.register(EMAIL, HASH, Role.VOLUNTEER);

		assertThatThrownBy(account::activate).isInstanceOf(DomainException.class);
		account.suspend();
		assertThatThrownBy(account::suspend).isInstanceOf(DomainException.class);
	}

	@Test
	void changesPassword() {
		Account account = Account.register(EMAIL, HASH, Role.VOLUNTEER);

		account.changePassword(PasswordHash.of("novo"));

		assertThat(account.passwordHash()).isEqualTo(PasswordHash.of("novo"));
		assertThatThrownBy(() -> account.changePassword(null)).isInstanceOf(DomainException.class);
	}

	@Test
	void restoredAccountRaisesNoEvents() {
		Account original = Account.register(EMAIL, HASH, Role.NGO);

		Account restored = Account.restore(original.id(), EMAIL, HASH, Role.NGO, AccountStatus.SUSPENDED);

		assertThat(restored).isEqualTo(original);
		assertThat(restored.domainEvents()).isEmpty();
		assertThat(restored.isActive()).isFalse();
	}
}
