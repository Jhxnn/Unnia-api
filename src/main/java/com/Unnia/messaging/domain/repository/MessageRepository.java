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

	/** Mensagens da conversa, da mais recente para a mais antiga. */
	List<Message> findByConversation(ConversationId conversationId, int page, int size);

	/** Mensagens da conversa ainda não lidas pelo destinatário. */
	List<Message> findUnread(ConversationId conversationId, AccountId recipientId);

	long countUnread(ConversationId conversationId, AccountId recipientId);

	long countUnreadFor(AccountId recipientId);
}
