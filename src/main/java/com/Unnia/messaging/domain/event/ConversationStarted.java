package com.Unnia.messaging.domain.event;

import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;

public record ConversationStarted(ConversationId conversationId, AccountId initiatorId, AccountId otherId)
		implements DomainEvent {
}
