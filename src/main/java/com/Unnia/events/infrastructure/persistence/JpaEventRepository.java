package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.Event;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
class JpaEventRepository implements EventRepository {

	private final EventJpaRepository jpa;

	JpaEventRepository(EventJpaRepository jpa) {
		this.jpa = jpa;
	}

	/**
	 * Reaproveita a entidade carregada (e sua versão) em vez de criar uma nova, para que o bloqueio
	 * otimista detecte alterações concorrentes no mesmo evento.
	 */
	@Override
	@Transactional
	public Event save(Event event) {
		EventEntity entity = jpa.findById(event.id().value()).orElseGet(EventEntity::new);
		EventMapper.apply(event, entity);
		jpa.save(entity);
		return event;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Event> findById(EventId id) {
		return jpa.findById(id.value()).map(EventMapper::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Event> findByOwner(NgoId ownerId) {
		return jpa.findByOwnerIdOrderByStartsAtDesc(ownerId.value()).stream().map(EventMapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Event> findByRegisteredVolunteer(VolunteerId volunteerId) {
		return jpa.findByActiveRegistration(volunteerId.value()).stream().map(EventMapper::toDomain).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Event> findUpcomingPublished(Instant from, int page, int size) {
		return jpa.findByStatusAndStartsAtAfterOrderByStartsAtAsc(EventStatus.PUBLISHED, from,
				PageRequest.of(page, size)).stream().map(EventMapper::toDomain).toList();
	}
}
