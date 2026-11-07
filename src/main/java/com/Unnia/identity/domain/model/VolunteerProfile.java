package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.VolunteerProfileCreated;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.Cep;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;
import com.Unnia.shared.domain.vo.StateCode;
import java.util.Optional;

public class VolunteerProfile extends AggregateRoot<VolunteerId> {

	private final AccountId accountId;
	private DisplayName name;
	private Description bio;
	private City city;
	private StateCode state;
	private Cep cep;

	private VolunteerProfile(VolunteerId id, AccountId accountId, DisplayName name, Description bio, City city,
			StateCode state, Cep cep) {
		super(id);
		this.accountId = Guard.notNull(accountId, "Conta é obrigatória");
		this.name = Guard.notNull(name, "Nome é obrigatório");
		this.bio = Guard.notNull(bio, "Bio é obrigatória");
		this.city = Guard.notNull(city, "Cidade é obrigatória");
		this.state = state;
		this.cep = cep;
	}

	public static VolunteerProfile create(Account account, DisplayName name, Description bio, City city) {
		return create(account, name, bio, city, null, null);
	}

	public static VolunteerProfile create(Account account, DisplayName name, Description bio, City city,
			StateCode state, Cep cep) {
		Guard.notNull(account, "Conta é obrigatória");
		Guard.check(account.hasRole(Role.VOLUNTEER),
				"Somente contas de voluntário podem ter perfil de voluntário");
		Guard.check(account.isActive(), "Conta precisa estar ativa");
		VolunteerProfile profile = new VolunteerProfile(VolunteerId.generate(), account.id(), name, bio, city, state,
				cep);
		profile.raise(new VolunteerProfileCreated(profile.id(), account.id()));
		return profile;
	}

	public static VolunteerProfile restore(VolunteerId id, AccountId accountId, DisplayName name,
			Description bio, City city, StateCode state, Cep cep) {
		return new VolunteerProfile(id, accountId, name, bio, city, state, cep);
	}

	/** Mantém o estado e o CEP que o perfil já tem. */
	public void updateDetails(DisplayName name, Description bio, City city) {
		updateDetails(name, bio, city, this.state, this.cep);
	}

	public void updateDetails(DisplayName name, Description bio, City city, StateCode state, Cep cep) {
		this.state = state;
		this.cep = cep;
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

	public Optional<StateCode> state() {
		return Optional.ofNullable(state);
	}

	public Optional<Cep> cep() {
		return Optional.ofNullable(cep);
	}

	public City city() {
		return city;
	}
}
