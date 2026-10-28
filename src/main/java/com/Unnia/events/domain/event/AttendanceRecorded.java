package com.Unnia.events.domain.event;

import com.Unnia.shared.domain.DomainEvent;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;

public record AttendanceRecorded(EventId eventId, VolunteerId volunteerId) implements DomainEvent {
}
