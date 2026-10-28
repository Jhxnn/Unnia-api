package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;

import java.time.Clock;

public class PublishEventUseCase {

	public record Command(AccountId accountId, EventId eventId) {
	}

	private final EventOwnership ownership;
	private final NgoDirectory ngos;
	private final EventRepository events;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public PublishEventUseCase(EventOwnership ownership, NgoDirectory ngos, EventRepository events,
			DomainEventPublisher publisher, Clock clock) {
		this.ownership = ownership;
		this.ngos = ngos;
		this.events = events;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		Event event = ownership.requireOwned(command.accountId(), command.eventId());
		if (!ngos.isVerified(event.ownerId())) {
			throw new ForbiddenException("A ONG precisa ser verificada para publicar eventos");
		}

		event.publish(clock.instant());

		events.save(event);
		publisher.publishFrom(event);
	}
}
