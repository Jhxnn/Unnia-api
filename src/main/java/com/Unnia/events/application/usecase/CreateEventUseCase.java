package com.Unnia.events.application.usecase;

import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.domain.model.Capacity;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventTitle;
import com.Unnia.events.domain.model.Location;
import com.Unnia.events.domain.model.Period;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;

import java.time.Clock;
import java.time.Instant;

public class CreateEventUseCase {

	public record Command(AccountId accountId, String title, String description, String address, String city,
			Instant start, Instant end, int capacity) {
	}

	private final NgoDirectory ngos;
	private final EventRepository events;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public CreateEventUseCase(NgoDirectory ngos, EventRepository events, DomainEventPublisher publisher,
			Clock clock) {
		this.ngos = ngos;
		this.events = events;
		this.publisher = publisher;
		this.clock = clock;
	}

	public EventId execute(Command command) {
		NgoId ownerId = ngos.findNgoIdByAccount(command.accountId())
				.orElseThrow(() -> new ForbiddenException("Somente ONGs podem criar eventos"));

		Event event = Event.create(ownerId, EventTitle.of(command.title()), Description.of(command.description()),
				Location.of(command.address(), City.of(command.city())), Period.of(command.start(), command.end()),
				Capacity.of(command.capacity()), clock.instant());

		events.save(event);
		publisher.publishFrom(event);
		return event.id();
	}
}
