package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageContent;
import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.ConversationEntity;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingMapperTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	@Test
	void canonicalOrderFollowsTheTextualOrderUsedByPostgres() {
		// Como inteiros com sinal o primeiro UUID é negativo (menor), mas o Postgres compara byte a byte
		// sem sinal e o considera MAIOR; a ordem textual (hex minúsculo) coincide com a do Postgres.
		UUID highByte = UUID.fromString("ffffffff-0000-0000-0000-000000000001");
		UUID lowByte = UUID.fromString("00000000-0000-0000-0000-000000000001");

		assertThat(highByte.compareTo(lowByte)).isNegative();
		assertThat(MessagingMapper.canonicalOrder(highByte, lowByte)).containsExactly(lowByte, highByte);
		assertThat(MessagingMapper.canonicalOrder(lowByte, highByte)).containsExactly(lowByte, highByte);
	}

	@Test
	void conversationIsStoredWithParticipantsInCanonicalOrderWhateverTheStartOrder() {
		AccountId first = AccountId.generate();
		AccountId second = AccountId.generate();

		ConversationEntity a = MessagingMapper.toEntity(Conversation.start(first, second, NOW));
		ConversationEntity b = MessagingMapper.toEntity(Conversation.start(second, first, NOW));

		assertThat(a.participantA).isEqualTo(b.participantA);
		assertThat(a.participantB).isEqualTo(b.participantB);
		assertThat(a.participantA.toString()).isLessThan(a.participantB.toString());
	}

	@Test
	void conversationRoundTripKeepsParticipantsAndActivity() {
		AccountId first = AccountId.generate();
		AccountId second = AccountId.generate();
		Conversation conversation = Conversation.start(first, second, NOW);
		conversation.send(first, MessageContent.of("oi"), NOW.plusSeconds(30));

		Conversation restored = MessagingMapper.toDomain(MessagingMapper.toEntity(conversation));

		assertThat(restored).isEqualTo(conversation);
		assertThat(restored.participants()).containsExactlyInAnyOrder(first, second);
		assertThat(restored.lastActivityAt()).isEqualTo(NOW.plusSeconds(30));
		assertThat(restored.startedAt()).isEqualTo(NOW);
	}

	@Test
	void messageRoundTripKeepsReadState() {
		AccountId sender = AccountId.generate();
		AccountId recipient = AccountId.generate();
		Message message = Conversation.start(sender, recipient, NOW).send(sender, MessageContent.of("Olá"), NOW);
		message.markRead(recipient, NOW.plusSeconds(5));

		Message restored = MessagingMapper.toDomain(MessagingMapper.toEntity(message));

		assertThat(restored).isEqualTo(message);
		assertThat(restored.senderId()).isEqualTo(sender);
		assertThat(restored.recipientId()).isEqualTo(recipient);
		assertThat(restored.content().text()).isEqualTo("Olá");
		assertThat(restored.readAt()).contains(NOW.plusSeconds(5));
	}

	@Test
	void notificationRoundTripKeepsTypeReferenceAndReadState() {
		UUID reference = UUID.randomUUID();
		Notification notification = Notification.create(AccountId.generate(), NotificationType.NEW_COMMENT,
				"Novo comentário", reference, NOW);
		Notification withoutReference = Notification.create(AccountId.generate(), NotificationType.NEW_FOLLOWER,
				"Seguidor", null, NOW);

		Notification restored = MessagingMapper.toDomain(MessagingMapper.toEntity(notification));
		Notification restoredWithout = MessagingMapper.toDomain(MessagingMapper.toEntity(withoutReference));

		assertThat(restored.type()).isEqualTo(NotificationType.NEW_COMMENT);
		assertThat(restored.referenceId()).contains(reference);
		assertThat(restored.isRead()).isFalse();
		assertThat(restoredWithout.referenceId()).isEmpty();
		assertThat(List.of(restored, restoredWithout)).allSatisfy(n -> assertThat(n.createdAt()).isEqualTo(NOW));
	}
}
