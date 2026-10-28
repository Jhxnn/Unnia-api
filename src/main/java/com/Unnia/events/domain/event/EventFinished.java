package com.Unnia.events.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.EventId;

public record EventFinished(EventId eventId) implements DomainEvent {
}
