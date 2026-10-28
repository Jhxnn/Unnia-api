package com.Unnia.shared.application;

import com.Unnia.shared.domain.DomainEvent;

/**
 * Reage a um evento de domínio, possivelmente de outro contexto. O mecanismo que entrega os
 * eventos aos handlers fica na infraestrutura.
 */
public interface DomainEventHandler<E extends DomainEvent> {

	Class<E> eventType();

	void handle(E event);
}
