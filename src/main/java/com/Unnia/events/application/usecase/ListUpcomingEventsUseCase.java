package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventView;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.domain.Guard;

import java.time.Clock;
import java.util.List;

/** Descoberta pública: eventos publicados que ainda não começaram. */
public class ListUpcomingEventsUseCase {

	public static final int MAX_PAGE_SIZE = 50;

	private final EventRepository events;
	private final Clock clock;

	public ListUpcomingEventsUseCase(EventRepository events, Clock clock) {
		this.events = events;
		this.clock = clock;
	}

	public List<EventView> execute(int page, int size) {
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);
		return events.findUpcomingPublished(clock.instant(), page, size).stream().map(EventView::from).toList();
	}
}
