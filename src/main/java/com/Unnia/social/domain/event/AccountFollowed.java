package com.Unnia.social.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;

public record AccountFollowed(AccountId followerId, AccountId followeeId) implements DomainEvent {
}
