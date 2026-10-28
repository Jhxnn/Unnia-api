package com.Unnia.moderation.domain.model;

import com.Unnia.moderation.domain.event.ReportDismissed;
import com.Unnia.moderation.domain.event.ReportFiled;
import com.Unnia.moderation.domain.event.ReportResolved;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Optional;

public class Report extends AggregateRoot<ReportId> {

	private final AccountId reporterId;
	private final ReportTarget target;
	private final ReportReason reason;
	private final String details;
	private final Instant createdAt;
	private ReportStatus status;
	private AccountId reviewedBy;
	private Instant reviewedAt;

	private Report(ReportId id, AccountId reporterId, ReportTarget target, ReportReason reason, String details,
			Instant createdAt, ReportStatus status, AccountId reviewedBy, Instant reviewedAt) {
		super(id);
		this.reporterId = Guard.notNull(reporterId, "Denunciante é obrigatório");
		this.target = Guard.notNull(target, "Alvo é obrigatório");
		this.reason = Guard.notNull(reason, "Motivo é obrigatório");
		this.details = details == null ? "" : Guard.maxLength(details.trim(), 1000,
				"Os detalhes devem ter no máximo 1000 caracteres");
		this.createdAt = Guard.notNull(createdAt, "Data é obrigatória");
		this.status = Guard.notNull(status, "Status é obrigatório");
		this.reviewedBy = reviewedBy;
		this.reviewedAt = reviewedAt;
	}

	public static Report file(AccountId reporterId, ReportTarget target, ReportReason reason, String details,
			Instant now) {
		Guard.notNull(reporterId, "Denunciante é obrigatório");
		Guard.notNull(target, "Alvo é obrigatório");
		Guard.check(!(target.type() == ReportTargetType.ACCOUNT && target.id().equals(reporterId.value())),
				"Não é possível denunciar a própria conta");
		if (reason == ReportReason.OTHER) {
			Guard.notBlank(details, "Descreva o motivo da denúncia");
		}
		Report report = new Report(ReportId.generate(), reporterId, target, reason, details, now,
				ReportStatus.OPEN, null, null);
		report.raise(new ReportFiled(report.id(), target));
		return report;
	}

	public static Report restore(ReportId id, AccountId reporterId, ReportTarget target, ReportReason reason,
			String details, Instant createdAt, ReportStatus status, AccountId reviewedBy, Instant reviewedAt) {
		return new Report(id, reporterId, target, reason, details, createdAt, status, reviewedBy, reviewedAt);
	}

	public void resolve(AccountId moderatorId, Instant now) {
		review(moderatorId, now, ReportStatus.RESOLVED);
		raise(new ReportResolved(id(), target));
	}

	public void dismiss(AccountId moderatorId, Instant now) {
		review(moderatorId, now, ReportStatus.DISMISSED);
		raise(new ReportDismissed(id()));
	}

	private void review(AccountId moderatorId, Instant now, ReportStatus newStatus) {
		Guard.notNull(moderatorId, "Moderador é obrigatório");
		Guard.check(status == ReportStatus.OPEN, "A denúncia já foi analisada");
		Guard.check(!moderatorId.equals(reporterId), "O denunciante não pode analisar a própria denúncia");
		this.status = newStatus;
		this.reviewedBy = moderatorId;
		this.reviewedAt = now;
	}

	public AccountId reporterId() {
		return reporterId;
	}

	public ReportTarget target() {
		return target;
	}

	public ReportReason reason() {
		return reason;
	}

	public String details() {
		return details;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public ReportStatus status() {
		return status;
	}

	public Optional<AccountId> reviewedBy() {
		return Optional.ofNullable(reviewedBy);
	}

	public Optional<Instant> reviewedAt() {
		return Optional.ofNullable(reviewedAt);
	}
}
