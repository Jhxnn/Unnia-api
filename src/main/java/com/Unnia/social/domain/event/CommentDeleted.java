package com.Unnia.social.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.social.domain.model.CommentId;

public record CommentDeleted(CommentId commentId) implements DomainEvent {
}
