package com.Unnia.messaging.infrastructure.realtime;

import com.Unnia.messaging.application.port.RealtimeUpdates;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.domain.id.AccountId;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongFunction;

/**
 * Canal de tempo real por SSE (Server-Sent Events): cada aba aberta mantém uma conexão HTTP longa e recebe os
 * avisos de novidade. Guarda as conexões na memória deste processo; com várias instâncias da API, seria preciso um
 * barramento (por exemplo, Redis) para o aviso chegar à instância onde a pessoa está conectada.
 *
 * <p>O aviso só sai depois do commit da transação que criou a novidade; antes disso o cliente buscaria o dado e
 * ainda não o encontraria. Um batimento periódico mantém a conexão aberta através de proxies e detecta clientes que
 * foram embora.
 */
@Component
public class SseHub implements RealtimeUpdates {

	private static final Logger log = LoggerFactory.getLogger(SseHub.class);

	static final int MAX_CONNECTIONS_PER_ACCOUNT = 5;
	private static final long HEARTBEAT_SECONDS = 25;
	/** As conexões são renovadas de tempos em tempos pelo cliente; sem prazo, uma conexão esquecida viveria para sempre. */
	private static final long CONNECTION_TIMEOUT_MS = 30 * 60 * 1000L;

	private final Map<UUID, Deque<SseEmitter>> connections = new ConcurrentHashMap<>();
	private final LongFunction<SseEmitter> emitters;
	private final int maxPerAccount;
	private ScheduledExecutorService heartbeat;

	@Autowired
	public SseHub() {
		this(SseEmitter::new, MAX_CONNECTIONS_PER_ACCOUNT);
	}

	/** Para testes: permite trocar a fábrica de conexões e o limite por conta. */
	SseHub(LongFunction<SseEmitter> emitters, int maxPerAccount) {
		this.emitters = emitters;
		this.maxPerAccount = maxPerAccount;
	}

	@PostConstruct
	void startHeartbeat() {
		heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "sse-heartbeat");
			thread.setDaemon(true);
			return thread;
		});
		heartbeat.scheduleAtFixedRate(this::ping, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
	}

	@PreDestroy
	void stop() {
		if (heartbeat != null) {
			heartbeat.shutdownNow();
		}
		connections.values().forEach(list -> list.forEach(SseEmitter::complete));
		connections.clear();
	}

	/** Abre uma conexão para a conta. Passando do limite, a mais antiga é encerrada (a aba esquecida cede à nova). */
	public SseEmitter subscribe(AccountId account) {
		SseEmitter emitter = emitters.apply(CONNECTION_TIMEOUT_MS);
		Deque<SseEmitter> list = connections.computeIfAbsent(account.value(), key -> new ArrayDeque<>());
		SseEmitter evicted = null;
		synchronized (list) {
			list.addLast(emitter);
			if (list.size() > maxPerAccount) {
				evicted = list.pollFirst();
			}
		}
		Runnable remove = () -> remove(account.value(), emitter);
		emitter.onCompletion(remove);
		emitter.onTimeout(remove);
		emitter.onError(error -> remove.run());
		if (evicted != null) {
			evicted.complete();
		}
		send(account.value(), emitter, SseEmitter.event().name("ready").data("{}"));
		return emitter;
	}

	@Override
	public void notifyChange(AccountId recipient, NotificationType type, UUID referenceId) {
		String payload = "{\"type\":\"" + type.name() + "\",\"referenceId\":"
				+ (referenceId == null ? "null" : "\"" + referenceId + "\"") + "}";
		Runnable push = () -> broadcast(recipient.value(), SseEmitter.event().name("update").data(payload));
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					push.run();
				}
			});
		} else {
			push.run();
		}
	}

	/** Quantas conexões a conta tem abertas. */
	public int connectionsOf(AccountId account) {
		Deque<SseEmitter> list = connections.get(account.value());
		if (list == null) {
			return 0;
		}
		synchronized (list) {
			return list.size();
		}
	}

	private void broadcast(UUID account, SseEmitter.SseEventBuilder event) {
		Deque<SseEmitter> list = connections.get(account);
		if (list == null) {
			return;
		}
		SseEmitter[] snapshot;
		synchronized (list) {
			snapshot = list.toArray(new SseEmitter[0]);
		}
		for (SseEmitter emitter : snapshot) {
			send(account, emitter, event);
		}
	}

	private void ping() {
		connections.forEach((account, list) -> {
			SseEmitter[] snapshot;
			synchronized (list) {
				snapshot = list.toArray(new SseEmitter[0]);
			}
			for (SseEmitter emitter : snapshot) {
				send(account, emitter, SseEmitter.event().comment("ping"));
			}
		});
	}

	private void send(UUID account, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
		try {
			emitter.send(event);
		} catch (IOException | IllegalStateException e) {
			// o cliente foi embora: descarta a conexão sem alarde
			log.debug("Conexão em tempo real descartada: {}", e.toString());
			remove(account, emitter);
		}
	}

	private void remove(UUID account, SseEmitter emitter) {
		Deque<SseEmitter> list = connections.get(account);
		if (list == null) {
			return;
		}
		synchronized (list) {
			list.remove(emitter);
			if (list.isEmpty()) {
				connections.remove(account, list);
			}
		}
	}
}
