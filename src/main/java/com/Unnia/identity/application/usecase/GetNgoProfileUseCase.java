package com.Unnia.identity.application.usecase;

import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.NgoId;

public class GetNgoProfileUseCase {

	public record View(NgoId ngoId, String name, String cnpj, String description, String city,
			VerificationStatus verificationStatus) {
	}

	private final NgoProfileRepository ngos;

	public GetNgoProfileUseCase(NgoProfileRepository ngos) {
		this.ngos = ngos;
	}

	public View execute(NgoId ngoId) {
		NgoProfile profile = ngos.findById(ngoId)
				.orElseThrow(() -> new NotFoundException("ONG não encontrada"));
		return new View(profile.id(), profile.name().value(), profile.cnpj().formatted(),
				profile.description().value(), profile.city().value(), profile.verificationStatus());
	}
}
