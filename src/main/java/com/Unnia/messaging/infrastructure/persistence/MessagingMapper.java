package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageContent;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.ConversationEntity;
import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.MessageEntity;
import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.NotificationEntity;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class MessagingMapper {

	private MessagingMapper() {
	}

	/**
	 * Ordena pelo texto do UUID, não por {@link UUID#compareTo}: o Postgres compara UUIDs byte a
	 * byte sem sinal, e a comparação textual (hex minúsculo de largura fixa) coincide com ela.
	 */
	static List<UUID> canonicalOrder(UUID first, UUID second) {
		return List.of(first, second).stream().sorted(Comparator.comparing(UUID::toString)).toList();
	}

	static ConversationEntity toEntity(Conversation conversation) {
		List<UUID> ordered = conversation.participants().stream().map(AccountId::value)
				.sorted(Comparator.comparing(UUID::toString)).toList();
		ConversationEntity entity = new ConversationEntity();
		entity.id = conversation.id().value();
		entity.participantA = ordered.get(0);
		entity.participantB = ordered.get(1);
		entity.startedAt = conversation.startedAt();
		entity.lastActivityAt = conversation.lastActivityAt();
		return entity;
	}

	static Conversation toDomain(ConversationEntity entity) {
		return Conversation.restore(new ConversationId(entity.id),
				Set.of(new AccountId(entity.participantA), new AccountId(entity.participantB)), entity.startedAt,
				entity.lastActivityAt);
	}

	static MessageEntity toEntity(Message message) {
		MessageEntity entity = new MessageEntity();
		entity.id = message.id().value();
		entity.conversationId = message.conversationId().value();
		entity.senderId = message.senderId().value();
		entity.recipientId = message.recipientId().value();
		entity.content = message.content().text();
		entity.sentAt = message.sentAt();
		entity.readAt = message.readAt().orElse(null);
		return entity;
	}

	static Message toDomain(MessageEntity entity) {
		return Message.restore(new MessageId(entity.id), new ConversationId(entity.conversationId),
				new AccountId(entity.senderId), new AccountId(entity.recipientId), new MessageContent(entity.content),
				entity.sentAt, entity.readAt);
	}

	static NotificationEntity toEntity(Notification notification) {
		NotificationEntity entity = new NotificationEntity();
		entity.id = notification.id().value();
		entity.recipientId = notification.recipientId().value();
		entity.type = notification.type();
		entity.text = notification.text();
		entity.referenceId = notification.referenceId().orElse(null);
		entity.createdAt = notification.createdAt();
		entity.readAt = notification.readAt().orElse(null);
		return entity;
	}

	static Notification toDomain(NotificationEntity entity) {
		return Notification.restore(new NotificationId(entity.id), new AccountId(entity.recipientId), entity.type,
				entity.text, entity.referenceId, entity.createdAt, entity.readAt);
	}
}
