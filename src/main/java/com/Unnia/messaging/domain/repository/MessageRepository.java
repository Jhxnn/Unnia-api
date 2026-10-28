package com.Unnia.messaging.domain.repository;

import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.Message;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;
import java.util.Optional;

public interface MessageRepository {

	Message save(Message message);

	Optional<Message> findById(MessageId id);

	List<Message> findByConversation(ConversationId conversationId, int page, int size);

	long countUnreadFor(AccountId recipientId);
}
