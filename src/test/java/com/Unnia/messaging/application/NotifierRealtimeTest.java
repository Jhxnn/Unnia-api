package com.Unnia.messaging.application;

import com.Unnia.messaging.application.port.RealtimeUpdates;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotifierRealtimeTest {

	private record Pushed(AccountId recipient, NotificationType type, UUID referenceId) {
	}

	private final List<Pushed> pushed = new ArrayList<>();
	private final List<Object> saved = new ArrayList<>();

	@Test
	void everyNotificationIsPushedToItsRecipientAfterBeingSaved() {
		var repository = new com.Unnia.messaging.application.fakes.InMemoryNotificationRepository();
		Notifier notifier = new Notifier(repository, new MutableClock(Instant.parse("2026-11-01T10:00:00Z")),
				(recipient, type, reference) -> {
					// quando o aviso sai, a notificação já está gravada
					saved.add(repository.countUnreadFor(recipient));
					pushed.add(new Pushed(recipient, type, reference));
				});
		AccountId maria = AccountId.generate();
		UUID conversation = UUID.randomUUID();

		notifier.notify(maria, NotificationType.NEW_MESSAGE, "Você recebeu uma nova mensagem", conversation);

		assertThat(pushed).containsExactly(new Pushed(maria, NotificationType.NEW_MESSAGE, conversation));
		assertThat(saved).containsExactly(1L);
	}

	@Test
	void withoutARealtimeChannelNotifyJustSaves() {
		var repository = new com.Unnia.messaging.application.fakes.InMemoryNotificationRepository();
		Notifier notifier = new Notifier(repository, new MutableClock(Instant.parse("2026-11-01T10:00:00Z")));
		AccountId maria = AccountId.generate();

		notifier.notify(maria, NotificationType.NEW_FOLLOWER, "Você tem um novo seguidor", null);

		assertThat(repository.countUnreadFor(maria)).isEqualTo(1L);
		assertThat(RealtimeUpdates.NONE).isNotNull();
	}
}
