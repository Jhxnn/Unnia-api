package com.Unnia.events.application;

import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;

/** Carrega um evento garantindo que ele pertence à ONG da conta autenticada. */
public class EventOwnership {

	private final NgoDirectory ngos;
	private final EventRepository events;

	public EventOwnership(NgoDirectory ngos, EventRepository events) {
		this.ngos = ngos;
		this.events = events;
	}

	public Event requireOwned(AccountId accountId, EventId eventId) {
		NgoId ngoId = ngos.findNgoIdByAccount(accountId)
				.orElseThrow(() -> new ForbiddenException("Somente ONGs podem gerenciar eventos"));
		Event event = events.findById(eventId).orElseThrow(() -> new NotFoundException("Evento não encontrado"));
		if (!event.ownerId().equals(ngoId)) {
			throw new ForbiddenException("O evento pertence a outra ONG");
		}
		return event;
	}
}
