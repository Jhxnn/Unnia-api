package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.domain.id.AccountId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptadores dos repositórios do domínio de mensagens sobre Spring Data JPA. */
final class JpaMessagingRepositories {

	private JpaMessagingRepositories() {
	}

	@Repository
	static class JpaConversationRepository implements ConversationRepository {

		private final ConversationJpaRepository jpa;

		JpaConversationRepository(ConversationJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Conversation save(Conversation conversation) {
			jpa.save(MessagingMapper.toEntity(conversation));
			return conversation;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Conversation> findById(ConversationId id) {
			return jpa.findById(id.value()).map(MessagingMapper::toDomain);
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Conversation> findBetween(AccountId first, AccountId second) {
			List<UUID> ordered = MessagingMapper.canonicalOrder(first.value(), second.value());
			return jpa.findByParticipantAAndParticipantB(ordered.get(0), ordered.get(1))
					.map(MessagingMapper::toDomain);
		}

		@Override
		@Transactional(readOnly = true)
		public List<Conversation> findByParticipant(AccountId accountId) {
			return jpa.findByParticipantAOrParticipantB(accountId.value(), accountId.value()).stream()
					.map(MessagingMapper::toDomain).toList();
		}
	}

	@Repository
	static class JpaMessageRepository implements MessageRepository {

		private final MessageJpaRepository jpa;

		JpaMessageRepository(MessageJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Message save(Message message) {
			jpa.save(MessagingMapper.toEntity(message));
			return message;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Message> findById(MessageId id) {
			return jpa.findById(id.value()).map(MessagingMapper::toDomain);
		}

		@Override
		@Transactional(readOnly = true)
		public List<Message> findByConversation(ConversationId conversationId, int page, int size) {
			return jpa.findByConversationIdOrderBySentAtDesc(conversationId.value(), PageRequest.of(page, size))
					.stream().map(MessagingMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public List<Message> findUnread(ConversationId conversationId, AccountId recipientId) {
			return jpa.findByConversationIdAndRecipientIdAndReadAtIsNull(conversationId.value(), recipientId.value())
					.stream().map(MessagingMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public long countUnread(ConversationId conversationId, AccountId recipientId) {
			return jpa.countByConversationIdAndRecipientIdAndReadAtIsNull(conversationId.value(),
					recipientId.value());
		}

		@Override
		@Transactional(readOnly = true)
		public long countUnreadFor(AccountId recipientId) {
			return jpa.countByRecipientIdAndReadAtIsNull(recipientId.value());
		}
	}

	@Repository
	static class JpaNotificationRepository implements NotificationRepository {

		private final NotificationJpaRepository jpa;

		JpaNotificationRepository(NotificationJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Notification save(Notification notification) {
			jpa.save(MessagingMapper.toEntity(notification));
			return notification;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Notification> findById(NotificationId id) {
			return jpa.findById(id.value()).map(MessagingMapper::toDomain);
		}

		@Override
		@Transactional(readOnly = true)
		public List<Notification> findByRecipient(AccountId recipientId, int page, int size) {
			return jpa.findByRecipientIdOrderByCreatedAtDesc(recipientId.value(), PageRequest.of(page, size))
					.stream().map(MessagingMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public List<Notification> findUnread(AccountId recipientId) {
			return jpa.findByRecipientIdAndReadAtIsNull(recipientId.value()).stream()
					.map(MessagingMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public long countUnreadFor(AccountId recipientId) {
			return jpa.countByRecipientIdAndReadAtIsNull(recipientId.value());
		}
	}
}
