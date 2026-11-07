package com.Unnia.identity.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.identity.application.usecase.ActivateAccountUseCase;
import com.Unnia.identity.application.usecase.RejectNgoUseCase;
import com.Unnia.identity.application.usecase.SuspendAccountUseCase;
import com.Unnia.identity.application.usecase.VerifyNgoUseCase;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Operações restritas à equipe da plataforma (a rota inteira exige o papel ADMIN). */
@Tag(name = "Administração")
@RestController
@RequestMapping("/api/admin")
class AdminController {

	private final VerifyNgoUseCase verifyNgo;
	private final RejectNgoUseCase rejectNgo;
	private final SuspendAccountUseCase suspendAccount;
	private final ActivateAccountUseCase activateAccount;

	AdminController(VerifyNgoUseCase verifyNgo, RejectNgoUseCase rejectNgo, SuspendAccountUseCase suspendAccount,
			ActivateAccountUseCase activateAccount) {
		this.verifyNgo = verifyNgo;
		this.rejectNgo = rejectNgo;
		this.suspendAccount = suspendAccount;
		this.activateAccount = activateAccount;
	}

	@PostMapping("/ngos/{id}/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void verify(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
		verifyNgo.execute(new VerifyNgoUseCase.Command(admin.accountId(), new NgoId(id)));
	}

	@PostMapping("/ngos/{id}/reject")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reject(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
		rejectNgo.execute(new RejectNgoUseCase.Command(admin.accountId(), new NgoId(id)));
	}

	@PostMapping("/accounts/{id}/suspend")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void suspend(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
		suspendAccount.execute(new SuspendAccountUseCase.Command(admin.accountId(), new AccountId(id)));
	}

	@PostMapping("/accounts/{id}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void activate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
		activateAccount.execute(new ActivateAccountUseCase.Command(admin.accountId(), new AccountId(id)));
	}
}
