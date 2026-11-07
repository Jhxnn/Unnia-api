package com.Unnia.identity.infrastructure.web;

import com.Unnia.identity.application.usecase.AuthenticateUseCase;
import com.Unnia.identity.application.usecase.ChangePasswordUseCase;
import com.Unnia.identity.application.usecase.EndSessionUseCase;
import com.Unnia.identity.application.usecase.RefreshSessionUseCase;
import com.Unnia.identity.application.usecase.RegisterNgoUseCase;
import com.Unnia.identity.application.usecase.ResendEmailVerificationUseCase;
import com.Unnia.identity.application.usecase.VerifyEmailUseCase;
import com.Unnia.identity.application.usecase.RegisterVolunteerUseCase;
import com.Unnia.identity.application.usecase.StartSessionUseCase;
import com.Unnia.identity.domain.model.Role;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.identity.infrastructure.security.JwtService;
import com.Unnia.identity.infrastructure.security.JwtService.IssuedToken;
import com.Unnia.shared.application.UnauthorizedException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import io.swagger.v3.oas.annotations.tags.Tag;
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
			@NotBlank String name, String description, @NotBlank String city, String state, String cep) {
	}

	record RegisterVolunteerRequest(@NotBlank @Email String email, @NotBlank String password,
			@NotBlank String name, String bio, @NotBlank String city, String state, String cep) {
	}

	record LoginRequest(@NotBlank String email, @NotBlank String password) {
	}

	record RefreshRequest(@NotBlank String refreshToken) {
	}

	record VerifyEmailRequest(@NotBlank String token) {
	}

	record ResendVerificationRequest(@NotBlank String email) {
	}

	record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {
	}

	record NgoRegistered(AccountId accountId, NgoId ngoId) {
	}

	record VolunteerRegistered(AccountId accountId, VolunteerId volunteerId) {
	}

	/** {@code token} é o JWT de acesso (curto); {@code refreshToken} renova a sessão (longo, de uso único). */
	record SessionResponse(String token, Instant expiresAt, String refreshToken, Instant refreshExpiresAt,
			AccountId accountId, Role role) {
	}

	private final RegisterNgoUseCase registerNgo;
	private final RegisterVolunteerUseCase registerVolunteer;
	private final AuthenticateUseCase authenticate;
	private final ChangePasswordUseCase changePassword;
	private final StartSessionUseCase startSession;
	private final RefreshSessionUseCase refreshSession;
	private final EndSessionUseCase endSession;
	private final VerifyEmailUseCase verifyEmail;
	private final ResendEmailVerificationUseCase resendVerification;
	private final JwtService jwt;

	AuthController(RegisterNgoUseCase registerNgo, RegisterVolunteerUseCase registerVolunteer,
			AuthenticateUseCase authenticate, ChangePasswordUseCase changePassword, StartSessionUseCase startSession,
			RefreshSessionUseCase refreshSession, EndSessionUseCase endSession, VerifyEmailUseCase verifyEmail,
			ResendEmailVerificationUseCase resendVerification, JwtService jwt) {
		this.registerNgo = registerNgo;
		this.registerVolunteer = registerVolunteer;
		this.authenticate = authenticate;
		this.changePassword = changePassword;
		this.startSession = startSession;
		this.refreshSession = refreshSession;
		this.endSession = endSession;
		this.verifyEmail = verifyEmail;
		this.resendVerification = resendVerification;
		this.jwt = jwt;
	}

	@PostMapping("/register/ngo")
	@ResponseStatus(HttpStatus.CREATED)
	NgoRegistered registerNgo(@Valid @RequestBody RegisterNgoRequest request) {
		var result = registerNgo.execute(new RegisterNgoUseCase.Command(request.email(), request.password(),
				request.cnpj(), request.name(), request.description(), request.city(), request.state(), request.cep()));
		return new NgoRegistered(result.accountId(), result.ngoId());
	}

	@PostMapping("/register/volunteer")
	@ResponseStatus(HttpStatus.CREATED)
	VolunteerRegistered registerVolunteer(@Valid @RequestBody RegisterVolunteerRequest request) {
		var result = registerVolunteer.execute(new RegisterVolunteerUseCase.Command(request.email(),
				request.password(), request.name(), request.bio(), request.city(), request.state(), request.cep()));
		return new VolunteerRegistered(result.accountId(), result.volunteerId());
	}

	/** Confirma o e-mail com o token do link recebido. Abrir o mesmo link de novo também responde 204. */
	@PostMapping("/verify-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		verifyEmail.execute(new VerifyEmailUseCase.Command(request.token()));
	}

	/** Reenvia o link. Responde 204 exista a conta ou não, para não revelar quais e-mails estão cadastrados. */
	@PostMapping("/resend-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
		resendVerification.execute(new ResendEmailVerificationUseCase.Command(request.email()));
	}

	@PostMapping("/login")
	SessionResponse login(@Valid @RequestBody LoginRequest request) {
		var identity = authenticate.execute(new AuthenticateUseCase.Command(request.email(), request.password()));
		var session = startSession.execute(identity.accountId());
		IssuedToken access = jwt.issue(identity.accountId(), identity.role());
		return new SessionResponse(access.value(), access.expiresAt(), session.refreshToken(), session.expiresAt(),
				identity.accountId(), identity.role());
	}

	/** Troca o token de renovação por um novo par. Cada token só funciona uma vez. */
	@PostMapping("/refresh")
	SessionResponse refresh(@Valid @RequestBody RefreshRequest request) {
		if (!(refreshSession.execute(request.refreshToken()) instanceof RefreshSessionUseCase.Refreshed renewed)) {
			throw new UnauthorizedException("Sessão inválida ou expirada");
		}
		IssuedToken access = jwt.issue(renewed.accountId(), renewed.role());
		return new SessionResponse(access.value(), access.expiresAt(), renewed.refreshToken(),
				renewed.refreshExpiresAt(), renewed.accountId(), renewed.role());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody RefreshRequest request) {
		endSession.execute(request.refreshToken());
	}

	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody ChangePasswordRequest request) {
		changePassword.execute(new ChangePasswordUseCase.Command(user.accountId(), request.currentPassword(),
				request.newPassword()));
	}
}
