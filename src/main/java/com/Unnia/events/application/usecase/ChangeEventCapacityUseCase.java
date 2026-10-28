package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.domain.model.Capacity;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;

public class ChangeEventCapacityUseCase {

	public record Command(AccountId accountId, EventId eventId, int capacity) {
	}

	private final EventOwnership ownership;
	private final EventRepository events;

	public ChangeEventCapacityUseCase(EventOwnership ownership, EventRepository events) {
		this.ownership = ownership;
		this.events = events;
	}

	public void execute(Command command) {
		Event event = ownership.requireOwned(command.accountId(), command.eventId());

		event.changeCapacity(Capacity.of(command.capacity()));

		events.save(event);
	}
}
