package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.Guard;

public record PostContent(String text) {

	public PostContent {
		text = Guard.maxLength(Guard.notBlank(text, "O conteúdo da postagem é obrigatório"), 3000,
				"A postagem deve ter no máximo 3000 caracteres");
	}

	public static PostContent of(String text) {
		return new PostContent(text);
	}
}
