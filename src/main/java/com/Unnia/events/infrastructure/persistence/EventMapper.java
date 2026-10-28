package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.Capacity;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventTitle;
import com.Unnia.events.domain.model.Location;
import com.Unnia.events.domain.model.Period;
import com.Unnia.events.domain.model.Registration;
import com.Unnia.events.domain.model.RegistrationId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.Description;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class EventMapper {

	private EventMapper() {
	}

	/**
	 * Copia o estado do agregado para a entidade informada, preservando a versão e as inscrições
	 * existentes (atualiza no lugar em vez de recriar), o que mantém o bloqueio otimista efetivo.
	 */
	static void apply(Event event, EventEntity entity) {
		entity.id = event.id().value();
		entity.ownerId = event.ownerId().value();
		entity.title = event.title().value();
		entity.description = event.description().value();
		entity.address = event.location().address();
		entity.city = event.location().city().value();
		entity.startsAt = event.period().start();
		entity.endsAt = event.period().end();
		entity.capacity = event.capacity().max();
		entity.status = event.status();

		Map<UUID, RegistrationEntity> existing = new HashMap<>();
		entity.registrations.forEach(r -> existing.put(r.id, r));
		for (Registration registration : event.registrations()) {
			RegistrationEntity target = existing.get(registration.id().value());
			if (target == null) {
				target = new RegistrationEntity();
				target.id = registration.id().value();
				entity.registrations.add(target);
			}
			target.volunteerId = registration.volunteerId().value();
			target.registeredAt = registration.registeredAt();
			target.status = registration.status();
		}
	}

	static Event toDomain(EventEntity entity) {
		return Event.restore(new EventId(entity.id), new NgoId(entity.ownerId), EventTitle.of(entity.title),
				Description.of(entity.description), Location.of(entity.address, City.of(entity.city)),
				Period.of(entity.startsAt, entity.endsAt), Capacity.of(entity.capacity), entity.status,
				entity.registrations.stream().map(EventMapper::toDomain).toList());
	}

	private static Registration toDomain(RegistrationEntity entity) {
		return Registration.restore(new RegistrationId(entity.id), new VolunteerId(entity.volunteerId),
				entity.registeredAt, entity.status);
	}
}
