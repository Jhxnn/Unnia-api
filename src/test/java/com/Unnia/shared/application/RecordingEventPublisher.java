package com.Unnia.shared.application;

import com.Unnia.shared.domain.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public class RecordingEventPublisher implements DomainEventPublisher {

	private final List<DomainEvent> published = new ArrayList<>();

	@Override
	public void publish(List<DomainEvent> events) {
		published.addAll(events);
	}

	public List<DomainEvent> published() {
		return List.copyOf(published);
	}

	public void clear() {
		published.clear();
	}
}
