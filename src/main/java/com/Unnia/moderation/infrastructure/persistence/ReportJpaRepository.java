package com.Unnia.moderation.infrastructure.persistence;

import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ReportJpaRepository extends JpaRepository<ReportEntity, UUID> {

	List<ReportEntity> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

	boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(UUID reporterId, ReportTargetType targetType,
			UUID targetId, ReportStatus status);
}
