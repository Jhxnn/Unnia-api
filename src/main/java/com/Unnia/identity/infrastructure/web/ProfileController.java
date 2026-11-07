package com.Unnia.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.identity.application.usecase.GetCurrentAccountUseCase;
import com.Unnia.identity.application.usecase.GetNgoProfileUseCase;
import com.Unnia.identity.application.usecase.GetVolunteerProfileUseCase;
import com.Unnia.identity.application.usecase.ListVerifiedNgosUseCase;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Perfis")
@RestController
class ProfileController {

	record UpdateNgoRequest(@NotBlank String name, String description, @NotBlank String city) {
	}

	record UpdateVolunteerRequest(@NotBlank String name, String bio, @NotBlank String city) {
	}

	private final GetCurrentAccountUseCase getCurrentAccount;
	private final GetNgoProfileUseCase getNgo;
	private final GetVolunteerProfileUseCase getVolunteer;
	private final ListVerifiedNgosUseCase listNgos;
	private final UpdateNgoProfileUseCase updateNgo;
	private final UpdateVolunteerProfileUseCase updateVolunteer;

	ProfileController(GetCurrentAccountUseCase getCurrentAccount, GetNgoProfileUseCase getNgo,
			GetVolunteerProfileUseCase getVolunteer, ListVerifiedNgosUseCase listNgos,
			UpdateNgoProfileUseCase updateNgo,
			UpdateVolunteerProfileUseCase updateVolunteer) {
		this.getCurrentAccount = getCurrentAccount;
		this.getNgo = getNgo;
		this.getVolunteer = getVolunteer;
		this.listNgos = listNgos;
		this.updateNgo = updateNgo;
		this.updateVolunteer = updateVolunteer;
	}

	/** A conta autenticada: papel, nome e o id do perfil que aparece em eventos e inscrições. */
	@GetMapping("/api/me")
	GetCurrentAccountUseCase.View me(@AuthenticationPrincipal AuthenticatedUser user) {
		return getCurrentAccount.execute(user.accountId());
	}

	/** Vitrine pública das ONGs verificadas, em ordem alfabética. */
	@GetMapping("/api/ngos")
	List<ListVerifiedNgosUseCase.NgoCard> ngos(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return listNgos.execute(page, size);
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
