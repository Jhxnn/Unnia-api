package com.Unnia.shared.infrastructure;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publica os eventos de domínio no contexto Spring. A entrega aos handlers só acontece depois do
 * commit da transação (ver {@link DomainEventDispatcher}).
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

	private final ApplicationEventPublisher publisher;

	SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void publish(List<DomainEvent> events) {
		events.forEach(publisher::publishEvent);
	}
}
