package com.Unnia.messaging.application.port;

import com.Unnia.shared.domain.id.AccountId;

import java.util.Collection;
import java.util.Map;
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

	/** Nome de exibição das contas informadas (ONG, voluntário ou equipe); contas sem nome ficam de fora. */
	Map<AccountId, String> namesOf(Collection<AccountId> accountIds);
}
