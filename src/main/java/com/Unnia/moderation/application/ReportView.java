package com.Unnia.moderation.application;

import com.Unnia.moderation.domain.model.Report;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportReason;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;

public record ReportView(ReportId reportId, AccountId reporterId, ReportTarget target, ReportReason reason,
		String details, Instant createdAt, ReportStatus status) {

	public static ReportView from(Report report) {
		return new ReportView(report.id(), report.reporterId(), report.target(), report.reason(), report.details(),
				report.createdAt(), report.status());
	}
}
