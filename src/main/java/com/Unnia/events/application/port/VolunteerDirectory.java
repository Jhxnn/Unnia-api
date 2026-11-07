package com.Unnia.events.application.port;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Visão do contexto de identidade que o contexto de eventos precisa conhecer sobre voluntários. */
public interface VolunteerDirectory {

	/** Como o voluntário se apresenta: o suficiente para a ONG saber quem está inscrito. */
	record VolunteerSummary(String name, String city) {
	}

	Optional<VolunteerId> findVolunteerIdByAccount(AccountId accountId);

	/** Resumo de cada voluntário informado; quem não existe mais simplesmente fica de fora do mapa. */
	Map<VolunteerId, VolunteerSummary> summariesOf(Collection<VolunteerId> volunteerIds);
}
