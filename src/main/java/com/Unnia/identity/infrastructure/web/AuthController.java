package com.Unnia.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.identity.application.usecase.AuthenticateUseCase;
import com.Unnia.identity.application.usecase.ChangePasswordUseCase;
import com.Unnia.identity.application.usecase.RegisterNgoUseCase;
import com.Unnia.identity.application.usecase.RegisterVolunteerUseCase;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.identity.infrastructure.security.JwtService;
import com.Unnia.identity.infrastructure.security.JwtService.IssuedToken;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
class AuthController {

	record RegisterNgoRequest(@NotBlank @Email String email, @NotBlank String password, @NotBlank String cnpj,
			@NotBlank String name, String description, @NotBlank String city) {
	}

	record RegisterVolunteerRequest(@NotBlank @Email String email, @NotBlank String password,
			@NotBlank String name, String bio, @NotBlank String city) {
	}

	record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
	}

	record NgoRegistered(AccountId accountId, NgoId ngoId) {
	}

	record VolunteerRegistered(AccountId accountId, VolunteerId volunteerId) {
	}

	record LoginResponse(String token, Instant expiresAt, AccountId accountId, Role role) {
	}

	private final RegisterNgoUseCase registerNgo;
	private final RegisterVolunteerUseCase registerVolunteer;
	private final AuthenticateUseCase authenticate;
	private final ChangePasswordUseCase changePassword;
	private final JwtService jwt;

	AuthController(RegisterNgoUseCase registerNgo, RegisterVolunteerUseCase registerVolunteer,
			AuthenticateUseCase authenticate, ChangePasswordUseCase changePassword, JwtService jwt) {
		this.registerNgo = registerNgo;
		this.registerVolunteer = registerVolunteer;
		this.authenticate = authenticate;
		this.changePassword = changePassword;
		this.jwt = jwt;
	}

	@PostMapping("/register/ngo")
	@ResponseStatus(HttpStatus.CREATED)
	NgoRegistered registerNgo(@Valid @RequestBody RegisterNgoRequest request) {
		var result = registerNgo.execute(new RegisterNgoUseCase.Command(request.email(), request.password(),
				request.cnpj(), request.name(), request.description(), request.city()));
		return new NgoRegistered(result.accountId(), result.ngoId());
	}

	@PostMapping("/register/volunteer")
	@ResponseStatus(HttpStatus.CREATED)
	VolunteerRegistered registerVolunteer(@Valid @RequestBody RegisterVolunteerRequest request) {
		var result = registerVolunteer.execute(new RegisterVolunteerUseCase.Command(request.email(),
				request.password(), request.name(), request.bio(), request.city()));
		return new VolunteerRegistered(result.accountId(), result.volunteerId());
	}

	@PostMapping("/login")
	LoginResponse login(@Valid @RequestBody LoginRequest request) {
		var result = authenticate.execute(new AuthenticateUseCase.Command(request.email(), request.password()));
		IssuedToken token = jwt.issue(result.accountId(), result.role());
		return new LoginResponse(token.value(), token.expiresAt(), result.accountId(), result.role());
	}

	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody ChangePasswordRequest request) {
		changePassword.execute(new ChangePasswordUseCase.Command(user.accountId(), request.currentPassword(),
				request.newPassword()));
	}
}
