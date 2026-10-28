package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record FollowId(UUID value) {

	public FollowId {
		Guard.notNull(value, "FollowId é obrigatório");
	}

	public static FollowId generate() {
		return new FollowId(UUID.randomUUID());
	}

	public static FollowId of(String raw) {
		try {
			return new FollowId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("FollowId inválido");
		}
	}
}
