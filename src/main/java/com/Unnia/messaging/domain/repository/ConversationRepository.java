package com.Unnia.messaging.domain.repository;

import com.Unnia.messaging.domain.model.Conversation;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

	Conversation save(Conversation conversation);

	Optional<Conversation> findById(ConversationId id);

	Optional<Conversation> findBetween(AccountId first, AccountId second);

	List<Conversation> findByParticipant(AccountId accountId);
}
