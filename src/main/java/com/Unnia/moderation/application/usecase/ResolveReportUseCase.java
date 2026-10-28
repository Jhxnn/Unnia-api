package com.Unnia.moderation.application.usecase;

import com.Unnia.moderation.application.ModeratorPolicy;
import com.Unnia.moderation.application.port.ContentTakedown;
import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.repository.ReportRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;

/** Dá a denúncia como procedente e, opcionalmente, retira o conteúdo denunciado. */
public class ResolveReportUseCase {

	public record Command(AccountId moderatorId, ReportId reportId, boolean takeDown) {
	}

	private final ModeratorPolicy policy;
	private final ReportRepository reports;
	private final ContentTakedown takedown;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public ResolveReportUseCase(ModeratorPolicy policy, ReportRepository reports, ContentTakedown takedown,
			DomainEventPublisher publisher, Clock clock) {
		this.policy = policy;
		this.reports = reports;
		this.takedown = takedown;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		policy.requireModerator(command.moderatorId());
		Report report = reports.findById(command.reportId())
				.orElseThrow(() -> new NotFoundException("Denúncia não encontrada"));

		report.resolve(command.moderatorId(), clock.instant());
		if (command.takeDown()) {
			takedown.takeDown(command.moderatorId(), report.target());
		}

		reports.save(report);
		publisher.publishFrom(report);
	}
}
