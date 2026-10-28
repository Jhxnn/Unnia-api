package com.Unnia.shared.infrastructure;

import com.Unnia.shared.application.DomainEventHandler;
import com.Unnia.shared.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entrega cada evento de domínio aos handlers registrados, somente depois do commit de quem o
 * originou. Cada handler roda em sua própria transação, e a falha de um não afeta os demais nem a
 * operação original, que já foi confirmada.
 */
@Component
class DomainEventDispatcher {

	private static final Logger log = LoggerFactory.getLogger(DomainEventDispatcher.class);

	private final Map<Class<?>, List<DomainEventHandler<?>>> handlersByType = new HashMap<>();
	private final TransactionTemplate isolated;

	DomainEventDispatcher(List<DomainEventHandler<?>> handlers, PlatformTransactionManager transactionManager) {
		handlers.forEach(h -> handlersByType.computeIfAbsent(h.eventType(), t -> new java.util.ArrayList<>()).add(h));
		this.isolated = new TransactionTemplate(transactionManager);
		this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void on(DomainEvent event) {
		for (DomainEventHandler<?> handler : handlersByType.getOrDefault(event.getClass(), List.of())) {
			try {
				isolated.executeWithoutResult(status -> dispatch(handler, event));
			} catch (RuntimeException e) {
				log.error("Falha ao tratar {} com {}", event.getClass().getSimpleName(),
						handler.getClass().getSimpleName(), e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void dispatch(DomainEventHandler<?> handler, DomainEvent event) {
		((DomainEventHandler<DomainEvent>) handler).handle(event);
	}
}
