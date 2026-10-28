package com.Unnia.messaging.application.port;

import com.Unnia.shared.domain.id.AccountId;

import java.util.Optional;

/** Visão do contexto de identidade que o contexto de mensagens precisa conhecer. */
public interface ParticipantDirectory {

	enum Kind {
		NGO,
		VOLUNTEER,
		OTHER
	}

	/** Tipo da conta, ou vazio se ela não existe ou não está ativa. */
	Optional<Kind> activeKindOf(AccountId accountId);
}
