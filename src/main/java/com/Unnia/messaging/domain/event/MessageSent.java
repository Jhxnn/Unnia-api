package com.Unnia.messaging.domain.event;

import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;

public record MessageSent(MessageId messageId, ConversationId conversationId, AccountId senderId,
		AccountId recipientId) implements DomainEvent {
}
