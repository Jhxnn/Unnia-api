package com.Unnia.social.application.port;

import com.Unnia.shared.domain.id.AccountId;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Visão do contexto de identidade que o contexto social usa para mostrar quem escreveu cada conteúdo. */
public interface AuthorDirectory {

	/**
	 * @param role      NGO, VOLUNTEER, ADMIN ou UNKNOWN (conta que já não existe)
	 * @param profileId id do perfil de ONG ou de voluntário; nulo para administradores e contas removidas
	 */
	record Author(String name, String role, UUID profileId) {
	}

	/** Usado quando a conta do autor já não existe. */
	Author UNKNOWN = new Author("Conta removida", "UNKNOWN", null);

	/** Os autores que existirem entre as contas informadas; as demais ficam de fora. */
	Map<AccountId, Author> authorsOf(Collection<AccountId> accountIds);
}
