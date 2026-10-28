package com.Unnia.moderation.application.usecase;

import com.Unnia.moderation.application.ModeratorPolicy;
import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.repository.ReportRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;

/** Arquiva a denúncia como improcedente, sem tocar no conteúdo. */
public class DismissReportUseCase {

	public record Command(AccountId moderatorId, ReportId reportId) {
	}

	private final ModeratorPolicy policy;
	private final ReportRepository reports;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public DismissReportUseCase(ModeratorPolicy policy, ReportRepository reports, DomainEventPublisher publisher,
			Clock clock) {
		this.policy = policy;
		this.reports = reports;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		policy.requireModerator(command.moderatorId());
		Report report = reports.findById(command.reportId())
				.orElseThrow(() -> new NotFoundException("Denúncia não encontrada"));

		report.dismiss(command.moderatorId(), clock.instant());

		reports.save(report);
		publisher.publishFrom(report);
	}
}
