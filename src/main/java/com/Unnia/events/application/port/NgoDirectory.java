package com.Unnia.events.application.port;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

import java.util.Optional;

/** Visão do contexto de identidade que o contexto de eventos precisa conhecer sobre ONGs. */
public interface NgoDirectory {

	Optional<NgoId> findNgoIdByAccount(AccountId accountId);

	boolean isVerified(NgoId ngoId);
}
