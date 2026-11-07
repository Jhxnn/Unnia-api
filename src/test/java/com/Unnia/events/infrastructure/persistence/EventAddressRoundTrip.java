package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.Event;

/** Passa um evento pelo mapeador (domínio, entidade, domínio), para os testes de outros pacotes verem o resultado. */
public final class EventAddressRoundTrip {

	private EventAddressRoundTrip() {
	}

	public static Event through(Event event) {
		EventEntity entity = new EventEntity();
		EventMapper.apply(event, entity);
		return EventMapper.toDomain(entity);
	}
}
