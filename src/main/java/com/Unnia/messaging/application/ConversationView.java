package com.Unnia.messaging.application;

import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;

public record ConversationView(ConversationId conversationId, AccountId otherAccountId, Instant lastActivityAt,
		long unreadCount) {
}
