package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.application.port.VolunteerDirectory;
import com.Unnia.events.application.port.VolunteerDirectory.VolunteerSummary;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.Registration;
import com.Unnia.events.domain.model.RegistrationStatus;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Quem está inscrito no evento, para a ONG organizadora: inscrições ativas (confirmadas ou com presença
 * registrada), da mais antiga para a mais recente. Só a ONG dona enxerga a lista.
 */
public class ListEventRegistrationsUseCase {

	public static final String UNKNOWN_VOLUNTEER = "Voluntário";

	public record Registrant(VolunteerId volunteerId, String name, String city, RegistrationStatus status,
			Instant registeredAt) {
	}

	private final EventOwnership ownership;
	private final VolunteerDirectory volunteers;

	public ListEventRegistrationsUseCase(EventOwnership ownership, VolunteerDirectory volunteers) {
		this.ownership = ownership;
		this.volunteers = volunteers;
	}

	public List<Registrant> execute(AccountId accountId, EventId eventId) {
		Event event = ownership.requireOwned(accountId, eventId);
		List<Registration> active = event.registrations().stream().filter(Registration::isActive)
				.sorted(Comparator.comparing(Registration::registeredAt)).toList();
		Map<VolunteerId, VolunteerSummary> people = volunteers
				.summariesOf(active.stream().map(Registration::volunteerId).toList());

		return active.stream().map(registration -> {
			VolunteerSummary person = people.get(registration.volunteerId());
			return new Registrant(registration.volunteerId(), person == null ? UNKNOWN_VOLUNTEER : person.name(),
					person == null ? "" : person.city(), registration.status(), registration.registeredAt());
		}).toList();
	}
}
