package com.Unnia.messaging.domain.model;

import com.Unnia.shared.domain.Guard;

public record MessageContent(String text) {

	public MessageContent {
		text = Guard.maxLength(Guard.notBlank(text, "A mensagem não pode ser vazia"), 2000,
				"A mensagem deve ter no máximo 2000 caracteres");
	}

	public static MessageContent of(String text) {
		return new MessageContent(text);
	}
}
