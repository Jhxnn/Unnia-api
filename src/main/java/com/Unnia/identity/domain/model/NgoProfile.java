package com.Unnia.identity.domain.model;

import com.Unnia.identity.domain.event.NgoProfileCreated;
import com.Unnia.identity.domain.event.NgoRejected;
import com.Unnia.identity.domain.event.NgoVerified;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class NgoProfile extends AggregateRoot<NgoId> {

	private final AccountId accountId;
	private final Cnpj cnpj;
	private DisplayName name;
	private Description description;
	private City city;
	private VerificationStatus verificationStatus;

	private NgoProfile(NgoId id, AccountId accountId, Cnpj cnpj, DisplayName name, Description description,
			City city, VerificationStatus verificationStatus) {
		super(id);
		this.accountId = Guard.notNull(accountId, "Conta é obrigatória");
		this.cnpj = Guard.notNull(cnpj, "CNPJ é obrigatório");
		this.name = Guard.notNull(name, "Nome é obrigatório");
		this.description = Guard.notNull(description, "Descrição é obrigatória");
		this.city = Guard.notNull(city, "Cidade é obrigatória");
		this.verificationStatus = Guard.notNull(verificationStatus, "Status de verificação é obrigatório");
	}

	public static NgoProfile create(Account account, Cnpj cnpj, DisplayName name, Description description,
			City city) {
		Guard.notNull(account, "Conta é obrigatória");
		Guard.check(account.hasRole(Role.NGO), "Somente contas de ONG podem ter perfil de ONG");
		Guard.check(account.isActive(), "Conta precisa estar ativa");
		NgoProfile profile = new NgoProfile(NgoId.generate(), account.id(), cnpj, name, description, city,
				VerificationStatus.PENDING);
		profile.raise(new NgoProfileCreated(profile.id(), account.id()));
		return profile;
	}

	public static NgoProfile restore(NgoId id, AccountId accountId, Cnpj cnpj, DisplayName name,
			Description description, City city, VerificationStatus verificationStatus) {
		return new NgoProfile(id, accountId, cnpj, name, description, city, verificationStatus);
	}

	public void updateDetails(DisplayName name, Description description, City city) {
		this.name = Guard.notNull(name, "Nome é obrigatório");
		this.description = Guard.notNull(description, "Descrição é obrigatória");
		this.city = Guard.notNull(city, "Cidade é obrigatória");
	}

	public void verify() {
		ensurePending();
		verificationStatus = VerificationStatus.VERIFIED;
		raise(new NgoVerified(id(), accountId));
	}

	public void reject() {
		ensurePending();
		verificationStatus = VerificationStatus.REJECTED;
		raise(new NgoRejected(id(), accountId));
	}

	private void ensurePending() {
		Guard.check(verificationStatus == VerificationStatus.PENDING, "Verificação já foi decidida");
	}

	public boolean isVerified() {
		return verificationStatus == VerificationStatus.VERIFIED;
	}

	public AccountId accountId() {
		return accountId;
	}

	public Cnpj cnpj() {
		return cnpj;
	}

	public DisplayName name() {
		return name;
	}

	public Description description() {
		return description;
	}

	public City city() {
		return city;
	}

	public VerificationStatus verificationStatus() {
		return verificationStatus;
	}
}
