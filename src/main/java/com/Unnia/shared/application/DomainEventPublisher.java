package com.Unnia.shared.application;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public interface DomainEventPublisher {

	void publish(List<DomainEvent> events);

	/** Publica e esvazia os eventos pendentes de todos os agregados informados. */
	default void publishFrom(AggregateRoot<?>... aggregates) {
		List<DomainEvent> events = new ArrayList<>();
		for (AggregateRoot<?> aggregate : aggregates) {
			events.addAll(aggregate.pullDomainEvents());
		}
		publish(events);
	}
}
