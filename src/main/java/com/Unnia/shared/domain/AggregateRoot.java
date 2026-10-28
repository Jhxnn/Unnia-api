package com.Unnia.shared.domain;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot<ID> extends Entity<ID> {

	private final List<DomainEvent> domainEvents = new ArrayList<>();

	protected AggregateRoot(ID id) {
		super(id);
	}

	protected void raise(DomainEvent event) {
		domainEvents.add(event);
	}

	public List<DomainEvent> domainEvents() {
		return List.copyOf(domainEvents);
	}

	public List<DomainEvent> pullDomainEvents() {
		List<DomainEvent> pulled = List.copyOf(domainEvents);
		domainEvents.clear();
		return pulled;
	}
}
