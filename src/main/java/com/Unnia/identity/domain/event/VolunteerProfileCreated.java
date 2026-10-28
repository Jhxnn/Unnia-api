package com.Unnia.identity.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

public record VolunteerProfileCreated(VolunteerId volunteerId, AccountId accountId) implements DomainEvent {
}
