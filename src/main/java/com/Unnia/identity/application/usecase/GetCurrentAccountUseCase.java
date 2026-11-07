package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.util.UUID;

/**
 * "Quem sou eu": a conta autenticada com os dados do seu perfil. O {@code profileId} é o id de ONG
 * ou de voluntário, conforme o papel, e é o que aparece em eventos e inscrições.
 */
public class GetCurrentAccountUseCase {

	/**
	 * @param profileId          nulo para administradores, que não têm perfil
	 * @param name               nulo para administradores
	 * @param verificationStatus só para ONGs; nulo nos demais papéis
	 */
	public record View(AccountId accountId, String email, Role role, UUID profileId, String name, String city,
			VerificationStatus verificationStatus) {
	}

	private final AccountRepository accounts;
	private final NgoProfileRepository ngos;
	private final VolunteerProfileRepository volunteers;

	public GetCurrentAccountUseCase(AccountRepository accounts, NgoProfileRepository ngos,
			VolunteerProfileRepository volunteers) {
		this.accounts = accounts;
		this.ngos = ngos;
		this.volunteers = volunteers;
	}

	public View execute(AccountId accountId) {
		Account account = accounts.findById(accountId)
				.orElseThrow(() -> new NotFoundException("Conta não encontrada"));
		return switch (account.role()) {
			case NGO -> ngos.findByAccountId(accountId).map(profile -> ngoView(account, profile))
					.orElseGet(() -> withoutProfile(account));
			case VOLUNTEER -> volunteers.findByAccountId(accountId).map(profile -> volunteerView(account, profile))
					.orElseGet(() -> withoutProfile(account));
			case ADMIN -> withoutProfile(account);
		};
	}

	private View ngoView(Account account, NgoProfile profile) {
		return new View(account.id(), account.email().value(), account.role(), profile.id().value(),
				profile.name().value(), profile.city().value(), profile.verificationStatus());
	}

	private View volunteerView(Account account, VolunteerProfile profile) {
		return new View(account.id(), account.email().value(), account.role(), profile.id().value(),
				profile.name().value(), profile.city().value(), null);
	}

	private View withoutProfile(Account account) {
		return new View(account.id(), account.email().value(), account.role(), null, null, null, null);
	}
}
