package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventTitle;
import com.Unnia.events.domain.model.Location;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;

public class UpdateEventDetailsUseCase {

	public record Command(AccountId accountId, EventId eventId, String title, String description, String address,
			String city) {
	}

	private final EventOwnership ownership;
	private final EventRepository events;

	public UpdateEventDetailsUseCase(EventOwnership ownership, EventRepository events) {
		this.ownership = ownership;
		this.events = events;
	}

	public void execute(Command command) {
		Event event = ownership.requireOwned(command.accountId(), command.eventId());

		event.updateDetails(EventTitle.of(command.title()), Description.of(command.description()),
				Location.of(command.address(), City.of(command.city())));

		events.save(event);
	}
}
