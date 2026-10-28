package com.Unnia.moderation.infrastructure.persistence;

import com.Unnia.moderation.domain.model.ReportReason;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports")
class ReportEntity {

	@Id
	UUID id;

	@Column(name = "reporter_id", nullable = false)
	UUID reporterId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false, length = 10)
	ReportTargetType targetType;

	@Column(name = "target_id", nullable = false)
	UUID targetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	ReportReason reason;

	@Column(nullable = false, length = 1000)
	String details;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	ReportStatus status;

	@Column(name = "reviewed_by")
	UUID reviewedBy;

	@Column(name = "reviewed_at")
	Instant reviewedAt;

	protected ReportEntity() {
	}
}
