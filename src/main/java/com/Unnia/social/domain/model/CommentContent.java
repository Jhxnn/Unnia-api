package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.Guard;

public record CommentContent(String text) {

	public CommentContent {
		text = Guard.maxLength(Guard.notBlank(text, "O conteúdo do comentário é obrigatório"), 1000,
				"O comentário deve ter no máximo 1000 caracteres");
	}

	public static CommentContent of(String text) {
		return new CommentContent(text);
	}
}
