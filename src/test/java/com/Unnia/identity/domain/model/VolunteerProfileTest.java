package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.VolunteerProfileCreated;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolunteerProfileTest {

	private Account volunteerAccount() {
		return Account.register(Email.of("vol@exemplo.com"), PasswordHash.of("hash"), Role.VOLUNTEER);
	}

	private VolunteerProfile profile(Account account) {
		return VolunteerProfile.create(account, DisplayName.of("Maria Silva"), Description.of("Gosto de ajudar"),
				City.of("Recife"));
	}

	@Test
	void createsProfileLinkedToAccountAndRaisesEvent() {
		Account account = volunteerAccount();

		VolunteerProfile profile = profile(account);

		assertThat(profile.accountId()).isEqualTo(account.id());
		assertThat(profile.name().value()).isEqualTo("Maria Silva");
		assertThat(profile.pullDomainEvents())
				.containsExactly(new VolunteerProfileCreated(profile.id(), account.id()));
	}

	@Test
	void rejectsAccountThatIsNotVolunteer() {
		Account ngo = Account.register(Email.of("ong@exemplo.com"), PasswordHash.of("hash"), Role.NGO);

		assertThatThrownBy(() -> profile(ngo)).isInstanceOf(DomainException.class);
	}

	@Test
	void rejectsSuspendedAccount() {
		Account account = volunteerAccount();
		account.suspend();

		assertThatThrownBy(() -> profile(account)).isInstanceOf(DomainException.class);
	}

	@Test
	void updatesEditableDetails() {
		VolunteerProfile profile = profile(volunteerAccount());

		profile.updateDetails(DisplayName.of("Maria Souza"), Description.of("Nova bio"), City.of("Olinda"));

		assertThat(profile.name().value()).isEqualTo("Maria Souza");
		assertThat(profile.bio().value()).isEqualTo("Nova bio");
		assertThat(profile.city().value()).isEqualTo("Olinda");
	}
}
