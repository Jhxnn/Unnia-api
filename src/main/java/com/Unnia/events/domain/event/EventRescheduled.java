package com.Unnia.events.domain.event;

import com.Unnia.events.domain.model.Period;
import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.List;

public record EventRescheduled(EventId eventId, Period newPeriod, List<VolunteerId> affectedVolunteers)
		implements DomainEvent {

	public EventRescheduled {
		affectedVolunteers = List.copyOf(affectedVolunteers);
	}
}
