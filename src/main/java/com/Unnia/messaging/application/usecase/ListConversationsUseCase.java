package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.ConversationView;
import com.Unnia.messaging.application.port.ParticipantDirectory;
import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Caixa de entrada: conversas da conta, da mais recente para a mais antiga, com mensagens não lidas. */
public class ListConversationsUseCase {

	static final String UNKNOWN_NAME = "Conta removida";

	private final ConversationRepository conversations;
	private final MessageRepository messages;
	private final ParticipantDirectory participants;

	public ListConversationsUseCase(ConversationRepository conversations, MessageRepository messages,
			ParticipantDirectory participants) {
		this.conversations = conversations;
		this.messages = messages;
		this.participants = participants;
	}

	public List<ConversationView> execute(AccountId accountId) {
		List<Conversation> mine = conversations.findByParticipant(accountId).stream()
				.sorted(Comparator.comparing(Conversation::lastActivityAt).reversed()).toList();
		Map<AccountId, String> names = participants
				.namesOf(mine.stream().map(c -> c.otherParticipant(accountId)).distinct().toList());
		return mine.stream()
				.map(c -> new ConversationView(c.id(), c.otherParticipant(accountId),
						names.getOrDefault(c.otherParticipant(accountId), UNKNOWN_NAME), c.lastActivityAt(),
						messages.countUnread(c.id(), accountId)))
				.toList();
	}
}
