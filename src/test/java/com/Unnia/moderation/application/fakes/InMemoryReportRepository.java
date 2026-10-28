package com.Unnia.moderation.application.fakes;

import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.repository.ReportRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryReportRepository implements ReportRepository {

	private final Map<ReportId, Report> store = new LinkedHashMap<>();

	@Override
	public Report save(Report report) {
		store.put(report.id(), report);
		return report;
	}

	@Override
	public Optional<Report> findById(ReportId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Report> findByStatus(ReportStatus status, int page, int size) {
		return store.values().stream().filter(r -> r.status() == status)
				.sorted(Comparator.comparing(Report::createdAt)).skip((long) page * size).limit(size).toList();
	}

	@Override
	public boolean existsOpenByReporterAndTarget(AccountId reporterId, ReportTarget target) {
		return store.values().stream().anyMatch(r -> r.status() == ReportStatus.OPEN
				&& r.reporterId().equals(reporterId) && r.target().equals(target));
	}
}
