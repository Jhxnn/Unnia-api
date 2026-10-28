package com.Unnia.identity.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;

public record AccountActivated(AccountId accountId) implements DomainEvent {
}
