package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.NotificationView;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;

public class ListNotificationsUseCase {

	public static final int MAX_PAGE_SIZE = 50;

	private final NotificationRepository notifications;

	public ListNotificationsUseCase(NotificationRepository notifications) {
		this.notifications = notifications;
	}

	public List<NotificationView> execute(AccountId accountId, int page, int size) {
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);
		return notifications.findByRecipient(accountId, page, size).stream().map(NotificationView::from).toList();
	}
}
