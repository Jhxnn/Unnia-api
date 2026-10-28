package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.port.ParticipantDirectory;
import com.Unnia.messaging.application.port.ParticipantDirectory.Kind;
import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageContent;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;
import java.time.Instant;

/**
 * Envia uma mensagem, abrindo a conversa se ainda não existir. Conversas são permitidas apenas
 * entre uma ONG e um voluntário.
 */
public class SendMessageUseCase {

	public record Command(AccountId senderId, AccountId recipientId, String text) {
	}

	public record Result(ConversationId conversationId, MessageId messageId) {
	}

	private final ParticipantDirectory participants;
	private final ConversationRepository conversations;
	private final MessageRepository messages;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public SendMessageUseCase(ParticipantDirectory participants, ConversationRepository conversations,
			MessageRepository messages, DomainEventPublisher publisher, Clock clock) {
		this.participants = participants;
		this.conversations = conversations;
		this.messages = messages;
		this.publisher = publisher;
		this.clock = clock;
	}

	public Result execute(Command command) {
		Kind sender = participants.activeKindOf(command.senderId())
				.orElseThrow(() -> new ForbiddenException("Conta do remetente indisponível"));
		Kind recipient = participants.activeKindOf(command.recipientId())
				.orElseThrow(() -> new NotFoundException("Destinatário não encontrado"));
		if (!isNgoAndVolunteer(sender, recipient)) {
			throw new ForbiddenException("Conversas são permitidas apenas entre ONGs e voluntários");
		}

		Instant now = clock.instant();
		Conversation conversation = conversations.findBetween(command.senderId(), command.recipientId())
				.orElseGet(() -> Conversation.start(command.senderId(), command.recipientId(), now));
		Message message = conversation.send(command.senderId(), MessageContent.of(command.text()), now);

		conversations.save(conversation);
		messages.save(message);
		publisher.publishFrom(conversation, message);
		return new Result(conversation.id(), message.id());
	}

	private boolean isNgoAndVolunteer(Kind first, Kind second) {
		return (first == Kind.NGO && second == Kind.VOLUNTEER) || (first == Kind.VOLUNTEER && second == Kind.NGO);
	}
}
