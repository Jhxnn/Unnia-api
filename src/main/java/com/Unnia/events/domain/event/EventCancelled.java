package com.Unnia.events.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.List;

/** Carrega os voluntários afetados para que possam ser notificados. */
public record EventCancelled(EventId eventId, List<VolunteerId> affectedVolunteers) implements DomainEvent {

	public EventCancelled {
		affectedVolunteers = List.copyOf(affectedVolunteers);
	}
}
