package com.Unnia.events.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;

public record RegistrationCancelled(EventId eventId, NgoId ownerId, VolunteerId volunteerId)
		implements DomainEvent {
}
