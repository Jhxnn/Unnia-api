package com.Unnia.social.application.fakes;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.port.MediaStorage;

import java.util.ArrayList;
import java.util.List;

/** Guarda o que foi enviado em memória; só reconhece como suas as URLs que ele mesmo devolveu. */
public class FakeMediaStorage implements MediaStorage {

	public static final String PREFIX = "https://cdn.test/unnia-media/posts/";

	public record Stored(AccountId owner, String extension, String contentType, int size, String url) {
	}

	private final List<Stored> stored = new ArrayList<>();
	private boolean enabled = true;

	public void disable() {
		enabled = false;
	}

	public List<Stored> stored() {
		return List.copyOf(stored);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String store(AccountId owner, String extension, String contentType, byte[] content) {
		String url = PREFIX + owner.value() + "/arquivo-" + (stored.size() + 1) + "." + extension;
		stored.add(new Stored(owner, extension, contentType, content.length, url));
		return url;
	}

	@Override
	public boolean owns(String url) {
		return enabled && url != null && url.startsWith(PREFIX);
	}

	/** Uma URL que o armazenamento reconhece, para quem só quer publicar uma postagem com imagem. */
	public String knownUrl() {
		return PREFIX + AccountId.generate().value() + "/qualquer.png";
	}
}
