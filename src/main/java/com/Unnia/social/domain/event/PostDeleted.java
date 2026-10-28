package com.Unnia.social.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.social.domain.model.PostId;

public record PostDeleted(PostId postId) implements DomainEvent {
}
