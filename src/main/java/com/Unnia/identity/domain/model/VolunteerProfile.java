package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.VolunteerProfileCreated;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class VolunteerProfile extends AggregateRoot<VolunteerId> {

	private final AccountId accountId;
	private DisplayName name;
	private Description bio;
	private City city;

	private VolunteerProfile(VolunteerId id, AccountId accountId, DisplayName name, Description bio, City city) {
		super(id);
		this.accountId = Guard.notNull(accountId, "Conta é obrigatória");
		this.name = Guard.notNull(name, "Nome é obrigatório");
		this.bio = Guard.notNull(bio, "Bio é obrigatória");
		this.city = Guard.notNull(city, "Cidade é obrigatória");
	}

	public static VolunteerProfile create(Account account, DisplayName name, Description bio, City city) {
		Guard.notNull(account, "Conta é obrigatória");
		Guard.check(account.hasRole(Role.VOLUNTEER),
				"Somente contas de voluntário podem ter perfil de voluntário");
		Guard.check(account.isActive(), "Conta precisa estar ativa");
		VolunteerProfile profile = new VolunteerProfile(VolunteerId.generate(), account.id(), name, bio, city);
		profile.raise(new VolunteerProfileCreated(profile.id(), account.id()));
		return profile;
	}

	public static VolunteerProfile restore(VolunteerId id, AccountId accountId, DisplayName name,
			Description bio, City city) {
		return new VolunteerProfile(id, accountId, name, bio, city);
	}

	public void updateDetails(DisplayName name, Description bio, City city) {
		this.name = Guard.notNull(name, "Nome é obrigatório");
		this.bio = Guard.notNull(bio, "Bio é obrigatória");
		this.city = Guard.notNull(city, "Cidade é obrigatória");
	}

	public AccountId accountId() {
		return accountId;
	}

	public DisplayName name() {
		return name;
	}

	public Description bio() {
		return bio;
	}

	public City city() {
		return city;
	}
}
