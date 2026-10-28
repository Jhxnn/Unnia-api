package com.Unnia.social.application.port;

import com.Unnia.shared.domain.id.AccountId;

/** Visão do contexto de identidade que o contexto social precisa conhecer. */
public interface AccountDirectory {

	/** Conta existente e ativa. */
	boolean isActive(AccountId accountId);
}
