package com.Unnia.moderation.domain.repository;

import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;
import java.util.Optional;

public interface ReportRepository {

	Report save(Report report);

	Optional<Report> findById(ReportId id);

	List<Report> findByStatus(ReportStatus status, int page, int size);

	boolean existsOpenByReporterAndTarget(AccountId reporterId, ReportTarget target);
}
