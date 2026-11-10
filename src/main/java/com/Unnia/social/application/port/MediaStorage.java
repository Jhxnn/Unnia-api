package com.Unnia.social.application.port;

import com.Unnia.shared.domain.id.AccountId;

/** Onde as imagens das postagens ficam guardadas (hoje, um bucket do Supabase Storage). */
public interface MediaStorage {

	/** Sem armazenamento configurado: nada é enviado e nenhuma URL de mídia é aceita. */
	MediaStorage DISABLED = new MediaStorage() {
		@Override
		public boolean isEnabled() {
			return false;
		}

		@Override
		public String store(AccountId owner, String extension, String contentType, byte[] content) {
			throw new IllegalStateException("Armazenamento de mídia não configurado");
		}

		@Override
		public boolean owns(String url) {
			return false;
		}
	};

	/** Há onde guardar? Falso quando as credenciais do armazenamento não foram informadas. */
	boolean isEnabled();

	/**
	 * Guarda o arquivo e devolve a URL pública dele.
	 *
	 * @param extension sem o ponto, por exemplo "png"
	 */
	String store(AccountId owner, String extension, String contentType, byte[] content);

	/** A URL aponta para um arquivo guardado por este armazenamento? Impede postagens com imagens de terceiros. */
	boolean owns(String url);
}
