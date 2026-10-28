package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.ConversationView;
import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Comparator;
import java.util.List;

/** Caixa de entrada: conversas da conta, da mais recente para a mais antiga, com mensagens não lidas. */
public class ListConversationsUseCase {

	private final ConversationRepository conversations;
	private final MessageRepository messages;

	public ListConversationsUseCase(ConversationRepository conversations, MessageRepository messages) {
		this.conversations = conversations;
		this.messages = messages;
	}

	public List<ConversationView> execute(AccountId accountId) {
		return conversations.findByParticipant(accountId).stream()
				.sorted(Comparator.comparing(Conversation::lastActivityAt).reversed())
				.map(c -> new ConversationView(c.id(), c.otherParticipant(accountId), c.lastActivityAt(),
						messages.countUnread(c.id(), accountId)))
				.toList();
	}
}
