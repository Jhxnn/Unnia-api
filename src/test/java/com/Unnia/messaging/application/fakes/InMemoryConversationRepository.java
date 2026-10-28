package com.Unnia.messaging.application.fakes;

import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.repository.ConversationRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryConversationRepository implements ConversationRepository {

	private final Map<ConversationId, Conversation> store = new LinkedHashMap<>();

	@Override
	public Conversation save(Conversation conversation) {
		store.put(conversation.id(), conversation);
		return conversation;
	}

	@Override
	public Optional<Conversation> findById(ConversationId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<Conversation> findBetween(AccountId first, AccountId second) {
		return store.values().stream().filter(c -> c.participants().equals(Set.of(first, second))).findFirst();
	}

	@Override
	public List<Conversation> findByParticipant(AccountId accountId) {
		return store.values().stream().filter(c -> c.includes(accountId)).toList();
	}

	public int size() {
		return store.size();
	}
}
