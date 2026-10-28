package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.time.Clock;

/** A ONG organizadora registra a presença de um voluntário inscrito. */
public class RecordAttendanceUseCase {

	public record Command(AccountId accountId, EventId eventId, VolunteerId volunteerId) {
	}

	private final EventOwnership ownership;
	private final EventRepository events;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public RecordAttendanceUseCase(EventOwnership ownership, EventRepository events,
			DomainEventPublisher publisher, Clock clock) {
		this.ownership = ownership;
		this.events = events;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		Event event = ownership.requireOwned(command.accountId(), command.eventId());

		event.recordAttendance(command.volunteerId(), clock.instant());

		events.save(event);
		publisher.publishFrom(event);
	}
}
