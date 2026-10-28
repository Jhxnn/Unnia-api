package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.MessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {

	List<MessageEntity> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);

	List<MessageEntity> findByConversationIdAndRecipientIdAndReadAtIsNull(UUID conversationId, UUID recipientId);

	long countByConversationIdAndRecipientIdAndReadAtIsNull(UUID conversationId, UUID recipientId);

	long countByRecipientIdAndReadAtIsNull(UUID recipientId);
}
