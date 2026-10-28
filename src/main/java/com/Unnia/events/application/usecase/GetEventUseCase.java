package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventView;
import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;

/** Rascunhos só são visíveis para a ONG dona; os demais eventos são públicos. */
public class GetEventUseCase {

	private final NgoDirectory ngos;
	private final EventRepository events;

	public GetEventUseCase(NgoDirectory ngos, EventRepository events) {
		this.ngos = ngos;
		this.events = events;
	}

	/** {@code requesterId} pode ser nulo para visitantes não autenticados. */
	public EventView execute(EventId eventId, AccountId requesterId) {
		Event event = events.findById(eventId).orElseThrow(() -> new NotFoundException("Evento não encontrado"));
		if (event.status() == EventStatus.DRAFT && !isOwner(event, requesterId)) {
			throw new NotFoundException("Evento não encontrado");
		}
		return EventView.from(event);
	}

	private boolean isOwner(Event event, AccountId requesterId) {
		return requesterId != null
				&& ngos.findNgoIdByAccount(requesterId).filter(event.ownerId()::equals).isPresent();
	}
}
