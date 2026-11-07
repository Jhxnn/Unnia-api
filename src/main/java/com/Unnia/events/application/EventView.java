package com.Unnia.events.application;

import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;

import java.time.Instant;

public record EventView(EventId eventId, NgoId ownerId, String title, String description, String address,
		String city, String state, String cep, Instant start, Instant end, int capacity, int occupiedSpots, int availableSpots,
		EventStatus status) {

	public static EventView from(Event event) {
		return new EventView(event.id(), event.ownerId(), event.title().value(), event.description().value(),
				event.location().address(), event.location().city().value(),
				event.location().state() == null ? null : event.location().state().value(),
				event.location().cep() == null ? null : event.location().cep().formatted(), event.period().start(),
				event.period().end(), event.capacity().max(), event.occupiedSpots(), event.availableSpots(),
				event.status());
	}
}
