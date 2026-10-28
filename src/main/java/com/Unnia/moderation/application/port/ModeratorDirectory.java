package com.Unnia.moderation.application.port;

import com.Unnia.shared.domain.id.AccountId;

/** Visão do contexto de identidade que o contexto de moderação precisa conhecer. */
public interface ModeratorDirectory {

	/** Conta existente, ativa e com papel de administrador. */
	boolean isModerator(AccountId accountId);
}
