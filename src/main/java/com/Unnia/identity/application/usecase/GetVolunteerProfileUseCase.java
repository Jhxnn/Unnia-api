package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.VolunteerProfile;
import com.Unnia.identity.domain.repository.VolunteerProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.VolunteerId;

public class GetVolunteerProfileUseCase {

	public record View(VolunteerId volunteerId, String name, String bio, String city) {
	}

	private final VolunteerProfileRepository volunteers;

	public GetVolunteerProfileUseCase(VolunteerProfileRepository volunteers) {
		this.volunteers = volunteers;
	}

	public View execute(VolunteerId volunteerId) {
		VolunteerProfile profile = volunteers.findById(volunteerId)
				.orElseThrow(() -> new NotFoundException("Voluntário não encontrado"));
		return new View(profile.id(), profile.name().value(), profile.bio().value(), profile.city().value());
	}
}
