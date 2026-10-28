package com.Unnia.messaging.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record NotificationId(UUID value) {

	public NotificationId {
		Guard.notNull(value, "NotificationId é obrigatório");
	}

	public static NotificationId generate() {
		return new NotificationId(UUID.randomUUID());
	}

	public static NotificationId of(String raw) {
		try {
			return new NotificationId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("NotificationId inválido");
		}
	}
}
