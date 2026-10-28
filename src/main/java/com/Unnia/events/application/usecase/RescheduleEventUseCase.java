package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.Period;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;

import java.time.Clock;
import java.time.Instant;

public class RescheduleEventUseCase {

	public record Command(AccountId accountId, EventId eventId, Instant start, Instant end) {
	}

	private final EventOwnership ownership;
	private final EventRepository events;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public RescheduleEventUseCase(EventOwnership ownership, EventRepository events, DomainEventPublisher publisher,
			Clock clock) {
		this.ownership = ownership;
		this.events = events;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		Event event = ownership.requireOwned(command.accountId(), command.eventId());

		event.reschedule(Period.of(command.start(), command.end()), clock.instant());

		events.save(event);
		publisher.publishFrom(event);
	}
}
