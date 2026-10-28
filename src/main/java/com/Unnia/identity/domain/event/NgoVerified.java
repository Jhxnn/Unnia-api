package com.Unnia.identity.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

public record NgoVerified(NgoId ngoId, AccountId accountId) implements DomainEvent {
}
