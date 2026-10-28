package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;
import com.Unnia.shared.domain.vo.DisplayName;

public class UpdateVolunteerProfileUseCase {

	public record Command(AccountId accountId, String name, String bio, String city) {
	}

	private final VolunteerProfileRepository volunteers;

	public UpdateVolunteerProfileUseCase(VolunteerProfileRepository volunteers) {
		this.volunteers = volunteers;
	}

	public void execute(Command command) {
		VolunteerProfile profile = volunteers.findByAccountId(command.accountId())
				.orElseThrow(() -> new NotFoundException("Perfil de voluntário não encontrado"));

		profile.updateDetails(DisplayName.of(command.name()), Description.of(command.bio()),
				City.of(command.city()));

		volunteers.save(profile);
	}
}
