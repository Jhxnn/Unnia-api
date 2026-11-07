package com.Unnia.identity.infrastructure.web;

import com.Unnia.identity.application.usecase.GetNgoProfileUseCase;
import com.Unnia.identity.application.usecase.GetVolunteerProfileUseCase;
import com.Unnia.identity.application.usecase.UpdateNgoProfileUseCase;
import com.Unnia.identity.application.usecase.UpdateVolunteerProfileUseCase;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class ProfileController {

	record UpdateNgoRequest(@NotBlank String name, String description, @NotBlank String city) {
	}

	record UpdateVolunteerRequest(@NotBlank String name, String bio, @NotBlank String city) {
	}

	private final GetNgoProfileUseCase getNgo;
	private final GetVolunteerProfileUseCase getVolunteer;
	private final UpdateNgoProfileUseCase updateNgo;
	private final UpdateVolunteerProfileUseCase updateVolunteer;

	ProfileController(GetNgoProfileUseCase getNgo, GetVolunteerProfileUseCase getVolunteer,
			UpdateNgoProfileUseCase updateNgo, UpdateVolunteerProfileUseCase updateVolunteer) {
		this.getNgo = getNgo;
		this.getVolunteer = getVolunteer;
		this.updateNgo = updateNgo;
		this.updateVolunteer = updateVolunteer;
	}

	@GetMapping("/api/ngos/{id}")
	GetNgoProfileUseCase.View ngo(@PathVariable UUID id) {
		return getNgo.execute(new NgoId(id));
	}

	@GetMapping("/api/volunteers/{id}")
	GetVolunteerProfileUseCase.View volunteer(@PathVariable UUID id) {
		return getVolunteer.execute(new VolunteerId(id));
	}

	@PutMapping("/api/ngos/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void updateNgo(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody UpdateNgoRequest request) {
		updateNgo.execute(new UpdateNgoProfileUseCase.Command(user.accountId(), request.name(),
				request.description(), request.city()));
	}

	@PutMapping("/api/volunteers/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void updateVolunteer(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody UpdateVolunteerRequest request) {
		updateVolunteer.execute(new UpdateVolunteerProfileUseCase.Command(user.accountId(), request.name(),
				request.bio(), request.city()));
	}
}
