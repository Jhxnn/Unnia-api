package com.Unnia.social.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.PostId;

public record PostPublished(PostId postId, AccountId authorId) implements DomainEvent {
}
