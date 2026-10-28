package com.Unnia.messaging.application.fakes;

import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.messaging.domain.repository.MessageRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryMessageRepository implements MessageRepository {

	private final Map<MessageId, Message> store = new LinkedHashMap<>();

	@Override
	public Message save(Message message) {
		store.put(message.id(), message);
		return message;
	}

	@Override
	public Optional<Message> findById(MessageId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Message> findByConversation(ConversationId conversationId, int page, int size) {
		return store.values().stream().filter(m -> m.conversationId().equals(conversationId))
				.sorted(Comparator.comparing(Message::sentAt).reversed())
				.skip((long) page * size).limit(size).toList();
	}

	@Override
	public List<Message> findUnread(ConversationId conversationId, AccountId recipientId) {
		return store.values().stream().filter(m -> m.conversationId().equals(conversationId)
				&& m.recipientId().equals(recipientId) && !m.isRead()).toList();
	}

	@Override
	public long countUnread(ConversationId conversationId, AccountId recipientId) {
		return findUnread(conversationId, recipientId).size();
	}

	@Override
	public long countUnreadFor(AccountId recipientId) {
		return store.values().stream().filter(m -> m.recipientId().equals(recipientId) && !m.isRead()).count();
	}
}
