package com.Unnia.events.application.fakes;

import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryEventRepository implements EventRepository {

	private final Map<EventId, Event> store = new LinkedHashMap<>();

	@Override
	public Event save(Event event) {
		store.put(event.id(), event);
		return event;
	}

	@Override
	public Optional<Event> findById(EventId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Event> findByOwner(NgoId ownerId) {
		return store.values().stream().filter(e -> e.ownerId().equals(ownerId)).toList();
	}

	@Override
	public List<Event> findByRegisteredVolunteer(VolunteerId volunteerId) {
		return store.values().stream().filter(e -> e.isRegistered(volunteerId)).toList();
	}

	@Override
	public List<Event> findUpcomingPublished(Instant from, int page, int size) {
		return store.values().stream()
				.filter(e -> e.status() == EventStatus.PUBLISHED && !e.period().hasStarted(from))
				.sorted(Comparator.comparing(e -> e.period().start()))
				.skip((long) page * size)
				.limit(size)
				.toList();
	}
}
