package com.Unnia.events.domain.repository;

import com.Unnia.events.domain.model.Event;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository {

	Event save(Event event);

	Optional<Event> findById(EventId id);

	List<Event> findByOwner(NgoId ownerId);

	List<Event> findByRegisteredVolunteer(VolunteerId volunteerId);

	/** Eventos publicados que ainda não começaram, do mais próximo para o mais distante. */
	List<Event> findUpcomingPublished(Instant from, int page, int size);
}
