package com.Unnia.moderation.infrastructure.persistence;

import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.repository.ReportRepository;
import com.Unnia.shared.domain.id.AccountId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
class JpaReportRepository implements ReportRepository {

	private final ReportJpaRepository jpa;

	JpaReportRepository(ReportJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Report save(Report report) {
		jpa.save(toEntity(report));
		return report;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Report> findById(ReportId id) {
		return jpa.findById(id.value()).map(JpaReportRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Report> findByStatus(ReportStatus status, int page, int size) {
		return jpa.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, size)).stream()
				.map(JpaReportRepository::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsOpenByReporterAndTarget(AccountId reporterId, ReportTarget target) {
		return jpa.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(reporterId.value(), target.type(),
				target.id(), ReportStatus.OPEN);
	}

	private static ReportEntity toEntity(Report report) {
		ReportEntity entity = new ReportEntity();
		entity.id = report.id().value();
		entity.reporterId = report.reporterId().value();
		entity.targetType = report.target().type();
		entity.targetId = report.target().id();
		entity.reason = report.reason();
		entity.details = report.details();
		entity.createdAt = report.createdAt();
		entity.status = report.status();
		entity.reviewedBy = report.reviewedBy().map(AccountId::value).orElse(null);
		entity.reviewedAt = report.reviewedAt().orElse(null);
		return entity;
	}

	private static Report toDomain(ReportEntity entity) {
		return Report.restore(new ReportId(entity.id), new AccountId(entity.reporterId),
				new ReportTarget(entity.targetType, entity.targetId), entity.reason, entity.details, entity.createdAt,
				entity.status, entity.reviewedBy == null ? null : new AccountId(entity.reviewedBy), entity.reviewedAt);
	}
}
