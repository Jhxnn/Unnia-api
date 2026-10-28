package com.Unnia.messaging.domain.repository;

import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

	Notification save(Notification notification);

	Optional<Notification> findById(NotificationId id);

	List<Notification> findByRecipient(AccountId recipientId, int page, int size);

	long countUnreadFor(AccountId recipientId);
}
