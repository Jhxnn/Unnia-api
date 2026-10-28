package com.Unnia.messaging.application.port;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.util.Optional;

/** Traduz ids de perfil, presentes nos eventos de eventos, na conta que deve receber a notificação. */
public interface AccountResolver {

	Optional<AccountId> accountOfNgo(NgoId ngoId);

	Optional<AccountId> accountOfVolunteer(VolunteerId volunteerId);
}
