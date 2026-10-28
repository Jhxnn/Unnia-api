package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.NotificationView;
import com.Unnia.messaging.application.fakes.InMemoryNotificationRepository;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationUseCasesTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private InMemoryNotificationRepository notifications;
	private Notifier notifier;
	private AccountId me;

	@BeforeEach
	void setUp() {
		notifications = new InMemoryNotificationRepository();
		notifier = new Notifier(notifications, clock);
		me = AccountId.generate();
	}

	private void notify(AccountId recipient, String text) {
		notifier.notify(recipient, NotificationType.NEW_FOLLOWER, text, UUID.randomUUID());
		clock.advance(Duration.ofMinutes(1));
	}

	@Test
	void listsOnlyMyNotificationsNewestFirstWithPaging() {
		notify(me, "antiga");
		notify(AccountId.generate(), "de outra pessoa");
		notify(me, "nova");
		ListNotificationsUseCase useCase = new ListNotificationsUseCase(notifications);

		List<NotificationView> page0 = useCase.execute(me, 0, 1);
		List<NotificationView> page1 = useCase.execute(me, 1, 1);

		assertThat(page0).extracting(NotificationView::text).containsExactly("nova");
		assertThat(page1).extracting(NotificationView::text).containsExactly("antiga");
		assertThat(page0.get(0).read()).isFalse();
	}

	@Test
	void listingValidatesPaging() {
		ListNotificationsUseCase useCase = new ListNotificationsUseCase(notifications);

		assertThatThrownBy(() -> useCase.execute(me, -1, 10)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(me, 0, 0)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(me, 0, ListNotificationsUseCase.MAX_PAGE_SIZE + 1))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void countsUnreadAndMarksOneAsRead() {
		notify(me, "a");
		notify(me, "b");
		NotificationId first = notifications.all().get(0).id();
		CountUnreadNotificationsUseCase count = new CountUnreadNotificationsUseCase(notifications);
		assertThat(count.execute(me)).isEqualTo(2);

		new MarkNotificationReadUseCase(notifications, clock)
				.execute(new MarkNotificationReadUseCase.Command(me, first));

		assertThat(count.execute(me)).isEqualTo(1);
	}

	@Test
	void cannotMarkSomeoneElsesOrUnknownNotification() {
		notify(me, "a");
		NotificationId id = notifications.all().get(0).id();
		MarkNotificationReadUseCase useCase = new MarkNotificationReadUseCase(notifications, clock);

		assertThatThrownBy(() -> useCase.execute(new MarkNotificationReadUseCase.Command(AccountId.generate(), id)))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(
				new MarkNotificationReadUseCase.Command(me, NotificationId.generate())))
				.isInstanceOf(NotFoundException.class);
		assertThat(notifications.countUnreadFor(me)).isEqualTo(1);
	}

	@Test
	void marksAllAsReadOnlyForTheCaller() {
		AccountId other = AccountId.generate();
		notify(me, "a");
		notify(me, "b");
		notify(other, "c");

		new MarkAllNotificationsReadUseCase(notifications, clock).execute(me);

		assertThat(notifications.countUnreadFor(me)).isZero();
		assertThat(notifications.countUnreadFor(other)).isEqualTo(1);
	}
}
