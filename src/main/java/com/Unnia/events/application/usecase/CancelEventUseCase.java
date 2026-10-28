package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;

public class CancelEventUseCase {

	public record Command(AccountId accountId, EventId eventId) {
	}

	private final EventOwnership ownership;
	private final EventRepository events;
	private final DomainEventPublisher publisher;

	public CancelEventUseCase(EventOwnership ownership, EventRepository events, DomainEventPublisher publisher) {
		this.ownership = ownership;
		this.events = events;
		this.publisher = publisher;
	}

	public void execute(Command command) {
		Event event = ownership.requireOwned(command.accountId(), command.eventId());

		event.cancel();

		events.save(event);
		publisher.publishFrom(event);
	}
}
