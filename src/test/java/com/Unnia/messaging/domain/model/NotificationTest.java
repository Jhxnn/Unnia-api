package com.Unnia.messaging.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final AccountId RECIPIENT = AccountId.generate();

	@Test
	void createsUnreadNotificationWithOptionalReference() {
		UUID reference = UUID.randomUUID();

		Notification notification = Notification.create(RECIPIENT, NotificationType.VOLUNTEER_REGISTERED,
				"Maria se inscreveu no seu evento", reference, NOW);

		assertThat(notification.recipientId()).isEqualTo(RECIPIENT);
		assertThat(notification.type()).isEqualTo(NotificationType.VOLUNTEER_REGISTERED);
		assertThat(notification.referenceId()).contains(reference);
		assertThat(notification.isRead()).isFalse();
		assertThat(notification.createdAt()).isEqualTo(NOW);
	}

	@Test
	void referenceIsOptional() {
		Notification notification = Notification.create(RECIPIENT, NotificationType.NEW_FOLLOWER, "Novo seguidor",
				null, NOW);

		assertThat(notification.referenceId()).isEmpty();
	}

	@Test
	void validatesTextAndRecipient() {
		assertThatThrownBy(() -> Notification.create(RECIPIENT, NotificationType.NEW_MESSAGE, " ", null, NOW))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Notification.create(RECIPIENT, NotificationType.NEW_MESSAGE, "x".repeat(301), null,
				NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Notification.create(null, NotificationType.NEW_MESSAGE, "ok", null, NOW))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Notification.create(RECIPIENT, null, "ok", null, NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void onlyRecipientMarksAsReadAndItIsIdempotent() {
		Notification notification = Notification.create(RECIPIENT, NotificationType.NEW_COMMENT, "Novo comentário",
				null, NOW);
		Instant firstRead = NOW.plusSeconds(5);

		assertThatThrownBy(() -> notification.markRead(AccountId.generate(), firstRead))
				.isInstanceOf(DomainException.class);
		notification.markRead(RECIPIENT, firstRead);
		notification.markRead(RECIPIENT, firstRead.plusSeconds(100));

		assertThat(notification.isRead()).isTrue();
		assertThat(notification.readAt()).contains(firstRead);
	}
}
