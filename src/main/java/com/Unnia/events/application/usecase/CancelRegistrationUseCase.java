package com.Unnia.events.application.usecase;

import com.Unnia.events.application.port.VolunteerDirectory;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.time.Clock;

public class CancelRegistrationUseCase {

	public record Command(AccountId accountId, EventId eventId) {
	}

	private final VolunteerDirectory volunteers;
	private final EventRepository events;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public CancelRegistrationUseCase(VolunteerDirectory volunteers, EventRepository events,
			DomainEventPublisher publisher, Clock clock) {
		this.volunteers = volunteers;
		this.events = events;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		VolunteerId volunteerId = volunteers.findVolunteerIdByAccount(command.accountId())
				.orElseThrow(() -> new ForbiddenException("Somente voluntários possuem inscrições"));
		Event event = events.findById(command.eventId())
				.orElseThrow(() -> new NotFoundException("Evento não encontrado"));

		event.cancelRegistration(volunteerId, clock.instant());

		events.save(event);
		publisher.publishFrom(event);
	}
}
