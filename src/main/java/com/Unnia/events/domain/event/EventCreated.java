package com.Unnia.events.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;

public record EventCreated(EventId eventId, NgoId ownerId) implements DomainEvent {
}
