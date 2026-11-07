package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.Cep;
import com.Unnia.shared.domain.vo.StateCode;

public class GetVolunteerProfileUseCase {

	public record View(VolunteerId volunteerId, AccountId accountId, String name, String bio, String city, String state,
			String cep) {
	}

	private final VolunteerProfileRepository volunteers;

	public GetVolunteerProfileUseCase(VolunteerProfileRepository volunteers) {
		this.volunteers = volunteers;
	}

	public View execute(VolunteerId volunteerId) {
		VolunteerProfile profile = volunteers.findById(volunteerId)
				.orElseThrow(() -> new NotFoundException("Voluntário não encontrado"));
		return new View(profile.id(), profile.accountId(), profile.name().value(), profile.bio().value(),
				profile.city().value(), profile.state().map(StateCode::value).orElse(null),
				profile.cep().map(Cep::formatted).orElse(null));
	}
}
