package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventView;
import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

import java.util.List;

/** Todos os eventos da própria ONG, inclusive rascunhos e cancelados. */
public class ListNgoEventsUseCase {

	private final NgoDirectory ngos;
	private final EventRepository events;

	public ListNgoEventsUseCase(NgoDirectory ngos, EventRepository events) {
		this.ngos = ngos;
		this.events = events;
	}

	public List<EventView> execute(AccountId accountId) {
		NgoId ngoId = ngos.findNgoIdByAccount(accountId)
				.orElseThrow(() -> new ForbiddenException("Somente ONGs possuem eventos próprios"));
		return events.findByOwner(ngoId).stream().map(EventView::from).toList();
	}
}
