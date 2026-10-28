package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.RawPassword;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class RegisterNgoUseCase {

	public record Command(String email, String password, String cnpj, String name, String description,
			String city) {
	}

	public record Result(AccountId accountId, NgoId ngoId) {
	}

	private final AccountRepository accounts;
	private final NgoProfileRepository ngos;
	private final PasswordEncoder passwordEncoder;
	private final DomainEventPublisher events;

	public RegisterNgoUseCase(AccountRepository accounts, NgoProfileRepository ngos,
			PasswordEncoder passwordEncoder, DomainEventPublisher events) {
		this.accounts = accounts;
		this.ngos = ngos;
		this.passwordEncoder = passwordEncoder;
		this.events = events;
	}

	public Result execute(Command command) {
		Email email = Email.of(command.email());
		Cnpj cnpj = Cnpj.of(command.cnpj());
		RawPassword password = RawPassword.of(command.password());

		if (accounts.existsByEmail(email)) {
			throw new ConflictException("E-mail já cadastrado");
		}
		if (ngos.existsByCnpj(cnpj)) {
			throw new ConflictException("CNPJ já cadastrado");
		}

		Account account = Account.register(email, passwordEncoder.encode(password), Role.NGO);
		NgoProfile profile = NgoProfile.create(account, cnpj, DisplayName.of(command.name()),
				Description.of(command.description()), City.of(command.city()));

		accounts.save(account);
		ngos.save(profile);
		events.publishFrom(account, profile);

		return new Result(account.id(), profile.id());
	}
}
