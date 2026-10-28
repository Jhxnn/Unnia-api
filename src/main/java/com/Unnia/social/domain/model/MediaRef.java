package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.Guard;

public record MediaRef(String url, MediaType type) {

	public MediaRef {
		url = Guard.notBlank(url, "URL da mídia é obrigatória");
		Guard.check(url.startsWith("http://") || url.startsWith("https://"), "URL da mídia inválida");
		Guard.check(url.length() <= 500, "URL da mídia muito longa");
		Guard.notNull(type, "Tipo da mídia é obrigatório");
	}

	public static MediaRef image(String url) {
		return new MediaRef(url, MediaType.IMAGE);
	}

	public static MediaRef video(String url) {
		return new MediaRef(url, MediaType.VIDEO);
	}
}
