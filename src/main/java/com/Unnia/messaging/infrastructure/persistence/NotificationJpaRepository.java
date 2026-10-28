package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

	List<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

	List<NotificationEntity> findByRecipientIdAndReadAtIsNull(UUID recipientId);

	long countByRecipientIdAndReadAtIsNull(UUID recipientId);
}
