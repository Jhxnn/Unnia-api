package com.Unnia.identity.domain.event;

import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;

public record AccountRegistered(AccountId accountId, Role role) implements DomainEvent {
}
