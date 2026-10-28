package com.Unnia.messaging.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Notification extends AggregateRoot<NotificationId> {

	private final AccountId recipientId;
	private final NotificationType type;
	private final String text;
	private final UUID referenceId;
	private final Instant createdAt;
	private Instant readAt;

	private Notification(NotificationId id, AccountId recipientId, NotificationType type, String text,
			UUID referenceId, Instant createdAt, Instant readAt) {
		super(id);
		this.recipientId = Guard.notNull(recipientId, "Destinatário é obrigatório");
		this.type = Guard.notNull(type, "Tipo é obrigatório");
		this.text = Guard.maxLength(Guard.notBlank(text, "Texto é obrigatório"), 300,
				"O texto deve ter no máximo 300 caracteres");
		this.referenceId = referenceId;
		this.createdAt = Guard.notNull(createdAt, "Data de criação é obrigatória");
		this.readAt = readAt;
	}

	public static Notification create(AccountId recipientId, NotificationType type, String text,
			UUID referenceId, Instant now) {
		return new Notification(NotificationId.generate(), recipientId, type, text, referenceId, now, null);
	}

	public static Notification restore(NotificationId id, AccountId recipientId, NotificationType type,
			String text, UUID referenceId, Instant createdAt, Instant readAt) {
		return new Notification(id, recipientId, type, text, referenceId, createdAt, readAt);
	}

	/** Idempotente: marcar uma notificação já lida não altera a data de leitura. */
	public void markRead(AccountId reader, Instant now) {
		Guard.check(recipientId.equals(reader), "Somente o destinatário pode marcar a notificação como lida");
		if (readAt == null) {
			readAt = now;
		}
	}

	public boolean isRead() {
		return readAt != null;
	}

	public AccountId recipientId() {
		return recipientId;
	}

	public NotificationType type() {
		return type;
	}

	public String text() {
		return text;
	}

	public Optional<UUID> referenceId() {
		return Optional.ofNullable(referenceId);
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Optional<Instant> readAt() {
		return Optional.ofNullable(readAt);
	}
}
