package com.Unnia.moderation.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.moderation.application.ReportView;
import com.Unnia.moderation.application.usecase.DismissReportUseCase;
import com.Unnia.moderation.application.usecase.FileReportUseCase;
import com.Unnia.moderation.application.usecase.ListOpenReportsUseCase;
import com.Unnia.moderation.application.usecase.ResolveReportUseCase;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportReason;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.model.ReportTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Denúncias")
@RestController
class ReportController {

	record FileReportRequest(@NotNull ReportTargetType targetType, @NotNull UUID targetId,
			@NotNull ReportReason reason, String details) {
	}

	record ResolveRequest(boolean takeDown) {
	}

	record ReportCreated(ReportId reportId) {
	}

	private final FileReportUseCase file;
	private final ListOpenReportsUseCase listOpen;
	private final ResolveReportUseCase resolve;
	private final DismissReportUseCase dismiss;

	ReportController(FileReportUseCase file, ListOpenReportsUseCase listOpen, ResolveReportUseCase resolve,
			DismissReportUseCase dismiss) {
		this.file = file;
		this.listOpen = listOpen;
		this.resolve = resolve;
		this.dismiss = dismiss;
	}

	@PostMapping("/api/reports")
	@ResponseStatus(HttpStatus.CREATED)
	ReportCreated file(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody FileReportRequest request) {
		return new ReportCreated(file.execute(new FileReportUseCase.Command(user.accountId(),
				new ReportTarget(request.targetType(), request.targetId()), request.reason(), request.details())));
	}

	@GetMapping("/api/admin/reports")
	List<ReportView> open(@AuthenticationPrincipal AuthenticatedUser admin,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return listOpen.execute(admin.accountId(), page, size);
	}

	@PostMapping("/api/admin/reports/{id}/resolve")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resolve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id,
			@RequestBody(required = false) ResolveRequest request) {
		boolean takeDown = request != null && request.takeDown();
		resolve.execute(new ResolveReportUseCase.Command(admin.accountId(), new ReportId(id), takeDown));
	}

	@PostMapping("/api/admin/reports/{id}/dismiss")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void dismiss(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
		dismiss.execute(new DismissReportUseCase.Command(admin.accountId(), new ReportId(id)));
	}
}
