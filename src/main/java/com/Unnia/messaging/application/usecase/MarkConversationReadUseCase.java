package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Clock;
import java.time.Instant;

public class MarkConversationReadUseCase {

	public record Command(AccountId accountId, ConversationId conversationId) {
	}

	private final ConversationRepository conversations;
	private final MessageRepository messages;
	private final Clock clock;

	public MarkConversationReadUseCase(ConversationRepository conversations, MessageRepository messages,
			Clock clock) {
		this.conversations = conversations;
		this.messages = messages;
		this.clock = clock;
	}

	public void execute(Command command) {
		Conversation conversation = conversations.findById(command.conversationId())
				.orElseThrow(() -> new NotFoundException("Conversa não encontrada"));
		if (!conversation.includes(command.accountId())) {
			throw new ForbiddenException("Você não participa desta conversa");
		}

		Instant now = clock.instant();
		for (Message message : messages.findUnread(conversation.id(), command.accountId())) {
			message.markRead(command.accountId(), now);
			messages.save(message);
		}
	}
}
