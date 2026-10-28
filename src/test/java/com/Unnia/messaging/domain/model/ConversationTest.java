package com.Unnia.messaging.domain.model;

import com.Unnia.messaging.domain.event.ConversationStarted;
import com.Unnia.messaging.domain.event.MessageSent;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversationTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final AccountId VOLUNTEER = AccountId.generate();
	private static final AccountId NGO = AccountId.generate();

	private Conversation conversation() {
		return Conversation.start(VOLUNTEER, NGO, NOW);
	}

	@Test
	void startsBetweenTwoDistinctAccountsAndRaisesEvent() {
		Conversation conversation = conversation();

		assertThat(conversation.participants()).containsExactlyInAnyOrder(VOLUNTEER, NGO);
		assertThat(conversation.startedAt()).isEqualTo(NOW);
		assertThat(conversation.lastActivityAt()).isEqualTo(NOW);
		assertThat(conversation.pullDomainEvents())
				.containsExactly(new ConversationStarted(conversation.id(), VOLUNTEER, NGO));
	}

	@Test
	void cannotTalkToSelfOrWithMissingParticipant() {
		assertThatThrownBy(() -> Conversation.start(VOLUNTEER, VOLUNTEER, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Conversation.start(null, NGO, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Conversation.start(VOLUNTEER, null, NOW)).isInstanceOf(DomainException.class);
	}

	@Test
	void restoreRequiresExactlyTwoParticipants() {
		assertThatThrownBy(() -> Conversation.restore(ConversationId.generate(), Set.of(VOLUNTEER), NOW, NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void knowsTheOtherParticipant() {
		Conversation conversation = conversation();

		assertThat(conversation.otherParticipant(VOLUNTEER)).isEqualTo(NGO);
		assertThat(conversation.otherParticipant(NGO)).isEqualTo(VOLUNTEER);
		assertThatThrownBy(() -> conversation.otherParticipant(AccountId.generate()))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void sendsMessageToTheOtherParticipantAndUpdatesActivity() {
		Conversation conversation = conversation();
		Instant later = NOW.plusSeconds(30);

		Message message = conversation.send(VOLUNTEER, MessageContent.of("Olá!"), later);

		assertThat(message.conversationId()).isEqualTo(conversation.id());
		assertThat(message.senderId()).isEqualTo(VOLUNTEER);
		assertThat(message.recipientId()).isEqualTo(NGO);
		assertThat(message.sentAt()).isEqualTo(later);
		assertThat(message.isRead()).isFalse();
		assertThat(conversation.lastActivityAt()).isEqualTo(later);
		assertThat(message.pullDomainEvents())
				.containsExactly(new MessageSent(message.id(), conversation.id(), VOLUNTEER, NGO));
	}

	@Test
	void strangersCannotSend() {
		assertThatThrownBy(() -> conversation().send(AccountId.generate(), MessageContent.of("oi"), NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void messageContentIsRequiredAndLimited() {
		assertThatThrownBy(() -> MessageContent.of("  ")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> MessageContent.of("x".repeat(2001))).isInstanceOf(DomainException.class);
	}

	@Test
	void onlyRecipientMarksMessageAsReadAndItIsIdempotent() {
		Message message = conversation().send(VOLUNTEER, MessageContent.of("Olá"), NOW);
		Instant firstRead = NOW.plusSeconds(10);

		assertThatThrownBy(() -> message.markRead(VOLUNTEER, firstRead)).isInstanceOf(DomainException.class);
		message.markRead(NGO, firstRead);
		message.markRead(NGO, firstRead.plusSeconds(500));

		assertThat(message.isRead()).isTrue();
		assertThat(message.readAt()).contains(firstRead);
	}
}
