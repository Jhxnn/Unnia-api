package com.Unnia.messaging.domain.repository;

import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

	Notification save(Notification notification);

	Optional<Notification> findById(NotificationId id);

	/** Notificações do destinatário, da mais recente para a mais antiga. */
	List<Notification> findByRecipient(AccountId recipientId, int page, int size);

	List<Notification> findUnread(AccountId recipientId);

	long countUnreadFor(AccountId recipientId);
}
