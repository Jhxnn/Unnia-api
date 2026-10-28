package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.AdminPolicy;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

public class VerifyNgoUseCase {

	public record Command(AccountId adminId, NgoId ngoId) {
	}

	private final AdminPolicy adminPolicy;
	private final NgoProfileRepository ngos;
	private final DomainEventPublisher events;

	public VerifyNgoUseCase(AdminPolicy adminPolicy, NgoProfileRepository ngos, DomainEventPublisher events) {
		this.adminPolicy = adminPolicy;
		this.ngos = ngos;
		this.events = events;
	}

	public void execute(Command command) {
		adminPolicy.requireAdmin(command.adminId());
		NgoProfile profile = ngos.findById(command.ngoId())
				.orElseThrow(() -> new NotFoundException("ONG não encontrada"));

		profile.verify();

		ngos.save(profile);
		events.publishFrom(profile);
	}
}
