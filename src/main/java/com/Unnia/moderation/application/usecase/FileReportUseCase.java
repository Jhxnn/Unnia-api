package com.Unnia.moderation.application.usecase;

import com.Unnia.moderation.application.port.ReportableContent;
import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportReason;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.repository.ReportRepository;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;

public class FileReportUseCase {

	public record Command(AccountId reporterId, ReportTarget target, ReportReason reason, String details) {
	}

	private final ReportableContent content;
	private final ReportRepository reports;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public FileReportUseCase(ReportableContent content, ReportRepository reports, DomainEventPublisher publisher,
			Clock clock) {
		this.content = content;
		this.reports = reports;
		this.publisher = publisher;
		this.clock = clock;
	}

	public ReportId execute(Command command) {
		if (!content.exists(command.target())) {
			throw new NotFoundException("O conteúdo denunciado não foi encontrado");
		}
		if (reports.existsOpenByReporterAndTarget(command.reporterId(), command.target())) {
			throw new ConflictException("Você já denunciou este conteúdo e a denúncia está em análise");
		}

		Report report = Report.file(command.reporterId(), command.target(), command.reason(), command.details(),
				clock.instant());

		reports.save(report);
		publisher.publishFrom(report);
		return report.id();
	}
}
