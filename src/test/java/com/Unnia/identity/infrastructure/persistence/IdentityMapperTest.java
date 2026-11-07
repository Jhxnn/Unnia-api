package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityMapperTest {

	@Test
	void accountSurvivesTheRoundTripAndKeepsItsStatus() {
		Account account = Account.register(Email.of("ong@exemplo.com"), PasswordHash.of("$2a$10$hash"), Role.NGO);
		account.suspend();

		AccountEntity entity = IdentityMapper.toEntity(account);
		Account restored = IdentityMapper.toDomain(entity);

		assertThat(entity.email).isEqualTo("ong@exemplo.com");
		assertThat(entity.role).isEqualTo(Role.NGO);
		assertThat(restored).isEqualTo(account);
		assertThat(restored.email()).isEqualTo(account.email());
		assertThat(restored.passwordHash()).isEqualTo(account.passwordHash());
		assertThat(restored.isActive()).isFalse();
		assertThat(restored.domainEvents()).isEmpty();
	}

	@Test
	void ngoProfileSurvivesTheRoundTripWithItsVerification() {
		Account account = Account.register(Email.of("ong@exemplo.com"), PasswordHash.of("h"), Role.NGO);
		NgoProfile profile = NgoProfile.create(account, Cnpj.of("11.222.333/0001-81"), DisplayName.of("Mãos"),
				Description.of("Desc"), City.of("Recife"));
		profile.verify();

		NgoProfileEntity entity = IdentityMapper.toEntity(profile);
		NgoProfile restored = IdentityMapper.toDomain(entity);

		assertThat(entity.cnpj).isEqualTo("11222333000181");
		assertThat(entity.verificationStatus).isEqualTo(VerificationStatus.VERIFIED);
		assertThat(restored.accountId()).isEqualTo(account.id());
		assertThat(restored.cnpj()).isEqualTo(profile.cnpj());
		assertThat(restored.name()).isEqualTo(profile.name());
		assertThat(restored.isVerified()).isTrue();
		assertThat(restored.domainEvents()).isEmpty();
	}

	@Test
	void volunteerProfileSurvivesTheRoundTrip() {
		Account account = Account.register(Email.of("vol@exemplo.com"), PasswordHash.of("h"), Role.VOLUNTEER);
		VolunteerProfile profile = VolunteerProfile.create(account, DisplayName.of("Maria"), Description.of("Bio"),
				City.of("Recife"));

		VolunteerProfile restored = IdentityMapper.toDomain(IdentityMapper.toEntity(profile));

		assertThat(restored).isEqualTo(profile);
		assertThat(restored.accountId()).isEqualTo(account.id());
		assertThat(restored.bio()).isEqualTo(profile.bio());
		assertThat(restored.city()).isEqualTo(profile.city());
	}
}
