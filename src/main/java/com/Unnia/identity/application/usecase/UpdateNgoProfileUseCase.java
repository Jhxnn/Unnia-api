package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class UpdateNgoProfileUseCase {

	public record Command(AccountId accountId, String name, String description, String city) {
	}

	private final NgoProfileRepository ngos;

	public UpdateNgoProfileUseCase(NgoProfileRepository ngos) {
		this.ngos = ngos;
	}

	public void execute(Command command) {
		NgoProfile profile = ngos.findByAccountId(command.accountId())
				.orElseThrow(() -> new NotFoundException("Perfil de ONG não encontrado"));

		profile.updateDetails(DisplayName.of(command.name()), Description.of(command.description()),
				City.of(command.city()));

		ngos.save(profile);
	}
}
