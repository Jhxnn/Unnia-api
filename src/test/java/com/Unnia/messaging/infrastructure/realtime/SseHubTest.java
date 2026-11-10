package com.Unnia.messaging.infrastructure.realtime;

import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SseHubTest {

	private final List<SseEmitter> created = new ArrayList<>();
	private SseHub hub;
	private AccountId maria;
	private AccountId joao;

	@BeforeEach
	void setUp() {
		created.clear();
		hub = new SseHub(timeout -> {
			SseEmitter emitter = mock(SseEmitter.class);
			created.add(emitter);
			return emitter;
		}, 2);
		maria = AccountId.generate();
		joao = AccountId.generate();
	}

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void subscribingOpensAConnectionAndSaysReady() throws IOException {
		SseEmitter emitter = hub.subscribe(maria);

		assertThat(emitter).isSameAs(created.get(0));
		assertThat(hub.connectionsOf(maria)).isEqualTo(1);
		verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	void anUpdateReachesOnlyTheRecipientsConnections() throws IOException {
		SseEmitter mariaTab1 = hub.subscribe(maria);
		SseEmitter mariaTab2 = hub.subscribe(maria);
		SseEmitter joaoTab = hub.subscribe(joao);

		hub.notifyChange(maria, NotificationType.NEW_MESSAGE, UUID.randomUUID());

		// ready + update nas duas abas da Maria; só o ready na do João
		verify(mariaTab1, times(2)).send(any(SseEmitter.SseEventBuilder.class));
		verify(mariaTab2, times(2)).send(any(SseEmitter.SseEventBuilder.class));
		verify(joaoTab, times(1)).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	void nobodyConnectedIsNotAnError() {
		hub.notifyChange(maria, NotificationType.NEW_FOLLOWER, null);

		assertThat(hub.connectionsOf(maria)).isZero();
	}

	@Test
	void aDeadConnectionIsDroppedWithoutAffectingTheOthers() throws IOException {
		SseEmitter dead = hub.subscribe(maria);
		SseEmitter alive = hub.subscribe(maria);
		doThrow(new IOException("cliente foi embora")).when(dead).send(any(SseEmitter.SseEventBuilder.class));

		hub.notifyChange(maria, NotificationType.NEW_COMMENT, UUID.randomUUID());

		assertThat(hub.connectionsOf(maria)).isEqualTo(1);
		verify(alive, times(2)).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	void passingTheLimitTheOldestConnectionIsClosed() {
		SseEmitter oldest = hub.subscribe(maria);
		hub.subscribe(maria);
		hub.subscribe(maria);

		assertThat(hub.connectionsOf(maria)).isEqualTo(2);
		verify(oldest).complete();
	}

	@Test
	void insideATransactionTheUpdateOnlyGoesOutAfterTheCommit() throws IOException {
		SseEmitter emitter = hub.subscribe(maria);
		TransactionSynchronizationManager.initSynchronization();

		hub.notifyChange(maria, NotificationType.NEW_MESSAGE, UUID.randomUUID());
		verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class)); // só o ready

		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
		verify(emitter, times(2)).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	void ifTheTransactionRollsBackNothingIsSent() throws IOException {
		SseEmitter emitter = hub.subscribe(maria);
		TransactionSynchronizationManager.initSynchronization();

		hub.notifyChange(maria, NotificationType.NEW_MESSAGE, UUID.randomUUID());
		TransactionSynchronizationManager.getSynchronizations()
				.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

		// só o "ready" da abertura; nenhum "update"
		verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
	}
}
