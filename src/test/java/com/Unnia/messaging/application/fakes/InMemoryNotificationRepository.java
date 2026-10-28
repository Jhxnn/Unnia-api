package com.Unnia.messaging.application.fakes;

import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.shared.domain.id.AccountId;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryNotificationRepository implements NotificationRepository {

	private final Map<NotificationId, Notification> store = new LinkedHashMap<>();

	@Override
	public Notification save(Notification notification) {
		store.put(notification.id(), notification);
		return notification;
	}

	@Override
	public Optional<Notification> findById(NotificationId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Notification> findByRecipient(AccountId recipientId, int page, int size) {
		return store.values().stream().filter(n -> n.recipientId().equals(recipientId))
				.sorted(Comparator.comparing(Notification::createdAt).reversed())
				.skip((long) page * size).limit(size).toList();
	}

	@Override
	public List<Notification> findUnread(AccountId recipientId) {
		return store.values().stream().filter(n -> n.recipientId().equals(recipientId) && !n.isRead()).toList();
	}

	@Override
	public long countUnreadFor(AccountId recipientId) {
		return findUnread(recipientId).size();
	}

	public List<Notification> all() {
		return List.copyOf(store.values());
	}
}
