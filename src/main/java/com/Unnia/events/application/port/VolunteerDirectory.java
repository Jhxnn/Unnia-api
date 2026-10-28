package com.Unnia.events.application.port;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.Optional;

/** Visão do contexto de identidade que o contexto de eventos precisa conhecer sobre voluntários. */
public interface VolunteerDirectory {

	Optional<VolunteerId> findVolunteerIdByAccount(AccountId accountId);
}
