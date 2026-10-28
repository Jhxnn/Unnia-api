package com.Unnia.social.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.ReactionId;
import com.Unnia.social.domain.model.ReactionTarget;

public record ReactionAdded(ReactionId reactionId, AccountId authorId, ReactionTarget target)
		implements DomainEvent {
}
