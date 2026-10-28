package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.NgoProfileCreated;
import com.Unnia.identity.domain.event.NgoRejected;
import com.Unnia.identity.domain.event.NgoVerified;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NgoProfileTest {

	private static final Cnpj CNPJ = Cnpj.of("11222333000181");

	private Account ngoAccount() {
		return Account.register(Email.of("ong@exemplo.com"), PasswordHash.of("hash"), Role.NGO);
	}

	private NgoProfile profile(Account account) {
		return NgoProfile.create(account, CNPJ, DisplayName.of("Mãos Solidárias"),
				Description.of("Ajudamos famílias"), City.of("Recife"));
	}

	@Test
	void createsPendingProfileLinkedToAccountAndRaisesEvent() {
		Account account = ngoAccount();

		NgoProfile profile = profile(account);

		assertThat(profile.accountId()).isEqualTo(account.id());
		assertThat(profile.cnpj()).isEqualTo(CNPJ);
		assertThat(profile.name().value()).isEqualTo("Mãos Solidárias");
		assertThat(profile.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
		assertThat(profile.isVerified()).isFalse();
		assertThat(profile.pullDomainEvents()).containsExactly(new NgoProfileCreated(profile.id(), account.id()));
	}

	@Test
	void rejectsAccountThatIsNotNgo() {
		Account volunteer = Account.register(Email.of("v@exemplo.com"), PasswordHash.of("hash"), Role.VOLUNTEER);

		assertThatThrownBy(() -> profile(volunteer)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsSuspendedAccount() {
		Account account = ngoAccount();
		account.suspend();

		assertThatThrownBy(() -> profile(account)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsMissingData() {
		Account account = ngoAccount();

		assertThatThrownBy(() -> NgoProfile.create(account, null, DisplayName.of("N"), Description.EMPTY,
				City.of("Recife"))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> NgoProfile.create(null, CNPJ, DisplayName.of("N"), Description.EMPTY,
				City.of("Recife"))).isInstanceOf(DomainException.class);
	}

	@Test
	void verifiesPendingProfile() {
		NgoProfile profile = profile(ngoAccount());
		profile.pullDomainEvents();

		profile.verify();

		assertThat(profile.isVerified()).isTrue();
		assertThat(profile.pullDomainEvents()).containsExactly(new NgoVerified(profile.id(), profile.accountId()));
	}

	@Test
	void rejectsPendingProfile() {
		NgoProfile profile = profile(ngoAccount());
		profile.pullDomainEvents();

		profile.reject();

		assertThat(profile.verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
		assertThat(profile.pullDomainEvents()).containsExactly(new NgoRejected(profile.id(), profile.accountId()));
	}

	@Test
	void decisionIsFinal() {
		NgoProfile verified = profile(ngoAccount());
		verified.verify();
		NgoProfile rejected = profile(ngoAccount());
		rejected.reject();

		assertThatThrownBy(verified::reject).isInstanceOf(DomainException.class);
		assertThatThrownBy(verified::verify).isInstanceOf(DomainException.class);
		assertThatThrownBy(rejected::verify).isInstanceOf(DomainException.class);
	}

	@Test
	void updatesEditableDetails() {
		NgoProfile profile = profile(ngoAccount());

		profile.updateDetails(DisplayName.of("Novo"), Description.of("Nova"), City.of("Olinda"));

		assertThat(profile.name().value()).isEqualTo("Novo");
		assertThat(profile.description().value()).isEqualTo("Nova");
		assertThat(profile.city().value()).isEqualTo("Olinda");
		assertThat(profile.cnpj()).isEqualTo(CNPJ);
	}
}
