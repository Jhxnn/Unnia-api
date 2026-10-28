package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventView;
import com.Unnia.events.application.port.VolunteerDirectory;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.List;

/** Eventos em que o voluntário autenticado tem inscrição ativa. */
public class ListMyRegistrationsUseCase {

	private final VolunteerDirectory volunteers;
	private final EventRepository events;

	public ListMyRegistrationsUseCase(VolunteerDirectory volunteers, EventRepository events) {
		this.volunteers = volunteers;
		this.events = events;
	}

	public List<EventView> execute(AccountId accountId) {
		VolunteerId volunteerId = volunteers.findVolunteerIdByAccount(accountId)
				.orElseThrow(() -> new ForbiddenException("Somente voluntários possuem inscrições"));
		return events.findByRegisteredVolunteer(volunteerId).stream().map(EventView::from).toList();
	}
}
