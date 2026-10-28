package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

/** Traduz entre os agregados de domínio e as entidades JPA. */
final class IdentityMapper {

	private IdentityMapper() {
	}

	static AccountEntity toEntity(Account account) {
		AccountEntity entity = new AccountEntity();
		entity.id = account.id().value();
		entity.email = account.email().value();
		entity.passwordHash = account.passwordHash().value();
		entity.role = account.role();
		entity.status = account.status();
		return entity;
	}

	static Account toDomain(AccountEntity entity) {
		return Account.restore(new AccountId(entity.id), Email.of(entity.email), PasswordHash.of(entity.passwordHash),
				entity.role, entity.status);
	}

	static NgoProfileEntity toEntity(NgoProfile profile) {
		NgoProfileEntity entity = new NgoProfileEntity();
		entity.id = profile.id().value();
		entity.accountId = profile.accountId().value();
		entity.cnpj = profile.cnpj().value();
		entity.name = profile.name().value();
		entity.description = profile.description().value();
		entity.city = profile.city().value();
		entity.verificationStatus = profile.verificationStatus();
		return entity;
	}

	static NgoProfile toDomain(NgoProfileEntity entity) {
		return NgoProfile.restore(new NgoId(entity.id), new AccountId(entity.accountId), Cnpj.of(entity.cnpj),
				DisplayName.of(entity.name), Description.of(entity.description), City.of(entity.city),
				entity.verificationStatus);
	}

	static VolunteerProfileEntity toEntity(VolunteerProfile profile) {
		VolunteerProfileEntity entity = new VolunteerProfileEntity();
		entity.id = profile.id().value();
		entity.accountId = profile.accountId().value();
		entity.name = profile.name().value();
		entity.bio = profile.bio().value();
		entity.city = profile.city().value();
		return entity;
	}

	static VolunteerProfile toDomain(VolunteerProfileEntity entity) {
		return VolunteerProfile.restore(new VolunteerId(entity.id), new AccountId(entity.accountId),
				DisplayName.of(entity.name), Description.of(entity.bio), City.of(entity.city));
	}
}
