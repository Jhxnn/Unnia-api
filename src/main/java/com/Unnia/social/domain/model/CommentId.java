package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.Guard;

import java.util.UUID;

public record CommentId(UUID value) {

	public CommentId {
		Guard.notNull(value, "CommentId é obrigatório");
	}

	public static CommentId generate() {
		return new CommentId(UUID.randomUUID());
	}

	public static CommentId of(String raw) {
		try {
			return new CommentId(UUID.fromString(raw));
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new DomainException("CommentId inválido");
		}
	}
}
