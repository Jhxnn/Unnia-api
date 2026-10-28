package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Entidades JPA do contexto de mensagens; detalhe de infraestrutura, invisível para o domínio. */
final class MessagingEntities {

	private MessagingEntities() {
	}

	/** Os participantes ficam em ordem canônica (a &lt; b) para haver uma única conversa por par. */
	@Entity
	@Table(name = "conversations")
	static class ConversationEntity {

		@Id
		UUID id;

		@Column(name = "participant_a", nullable = false)
		UUID participantA;

		@Column(name = "participant_b", nullable = false)
		UUID participantB;

		@Column(name = "started_at", nullable = false)
		Instant startedAt;

		@Column(name = "last_activity_at", nullable = false)
		Instant lastActivityAt;

		protected ConversationEntity() {
		}
	}

	@Entity
	@Table(name = "messages")
	static class MessageEntity {

		@Id
		UUID id;

		@Column(name = "conversation_id", nullable = false)
		UUID conversationId;

		@Column(name = "sender_id", nullable = false)
		UUID senderId;

		@Column(name = "recipient_id", nullable = false)
		UUID recipientId;

		@Column(nullable = false, length = 2000)
		String content;

		@Column(name = "sent_at", nullable = false)
		Instant sentAt;

		@Column(name = "read_at")
		Instant readAt;

		protected MessageEntity() {
		}
	}

	@Entity
	@Table(name = "notifications")
	static class NotificationEntity {

		@Id
		UUID id;

		@Column(name = "recipient_id", nullable = false)
		UUID recipientId;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 40)
		NotificationType type;

		@Column(nullable = false, length = 300)
		String text;

		@Column(name = "reference_id")
		UUID referenceId;

		@Column(name = "created_at", nullable = false)
		Instant createdAt;

		@Column(name = "read_at")
		Instant readAt;

		protected NotificationEntity() {
		}
	}
}
