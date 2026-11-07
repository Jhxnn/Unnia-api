package com.Unnia.messaging.infrastructure.web;

import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.messaging.application.NotificationView;
import com.Unnia.messaging.application.usecase.CountUnreadNotificationsUseCase;
import com.Unnia.messaging.application.usecase.ListNotificationsUseCase;
import com.Unnia.messaging.application.usecase.MarkAllNotificationsReadUseCase;
import com.Unnia.messaging.application.usecase.MarkNotificationReadUseCase;
import com.Unnia.messaging.domain.model.NotificationId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
class NotificationController {

	record UnreadCount(long count) {
	}

	private final ListNotificationsUseCase list;
	private final CountUnreadNotificationsUseCase countUnread;
	private final MarkNotificationReadUseCase markRead;
	private final MarkAllNotificationsReadUseCase markAllRead;

	NotificationController(ListNotificationsUseCase list, CountUnreadNotificationsUseCase countUnread,
			MarkNotificationReadUseCase markRead, MarkAllNotificationsReadUseCase markAllRead) {
		this.list = list;
		this.countUnread = countUnread;
		this.markRead = markRead;
		this.markAllRead = markAllRead;
	}

	@GetMapping
	List<NotificationView> list(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return list.execute(user.accountId(), page, size);
	}

	@GetMapping("/unread-count")
	UnreadCount unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
		return new UnreadCount(countUnread.execute(user.accountId()));
	}

	@PostMapping("/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void read(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		markRead.execute(new MarkNotificationReadUseCase.Command(user.accountId(), new NotificationId(id)));
	}

	@PostMapping("/read-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void readAll(@AuthenticationPrincipal AuthenticatedUser user) {
		markAllRead.execute(user.accountId());
	}
}
