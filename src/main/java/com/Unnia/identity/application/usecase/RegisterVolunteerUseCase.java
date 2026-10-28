package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.Email;
import com.Unnia.identity.domain.model.RawPassword;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class RegisterVolunteerUseCase {

	public record Command(String email, String password, String name, String bio, String city) {
	}

	public record Result(AccountId accountId, VolunteerId volunteerId) {
	}

	private final AccountRepository accounts;
	private final VolunteerProfileRepository volunteers;
	private final PasswordEncoder passwordEncoder;
	private final DomainEventPublisher events;

	public RegisterVolunteerUseCase(AccountRepository accounts, VolunteerProfileRepository volunteers,
			PasswordEncoder passwordEncoder, DomainEventPublisher events) {
		this.accounts = accounts;
		this.volunteers = volunteers;
		this.passwordEncoder = passwordEncoder;
		this.events = events;
	}

	public Result execute(Command command) {
		Email email = Email.of(command.email());
		RawPassword password = RawPassword.of(command.password());

		if (accounts.existsByEmail(email)) {
			throw new ConflictException("E-mail já cadastrado");
		}

		Account account = Account.register(email, passwordEncoder.encode(password), Role.VOLUNTEER);
		VolunteerProfile profile = VolunteerProfile.create(account, DisplayName.of(command.name()),
				Description.of(command.bio()), City.of(command.city()));

		accounts.save(account);
		volunteers.save(profile);
		events.publishFrom(account, profile);

		return new Result(account.id(), profile.id());
	}
}
