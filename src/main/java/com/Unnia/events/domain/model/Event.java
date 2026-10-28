package com.Unnia.events.domain.model;

import com.Unnia.events.domain.event.AttendanceRecorded;
import com.Unnia.events.domain.event.EventCancelled;
import com.Unnia.events.domain.event.EventCreated;
import com.Unnia.events.domain.event.EventFinished;
import com.Unnia.events.domain.event.EventPublished;
import com.Unnia.events.domain.event.EventRescheduled;
import com.Unnia.events.domain.event.RegistrationCancelled;
import com.Unnia.events.domain.event.VolunteerRegistered;
import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.shared.domain.vo.Description;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agregado raiz do contexto de eventos. As inscrições vivem dentro dele para que a
 * regra "nunca ultrapassar as vagas" seja garantida em uma única transação.
 */
public class Event extends AggregateRoot<EventId> {

	private final NgoId ownerId;
	private EventTitle title;
	private Description description;
	private Location location;
	private Period period;
	private Capacity capacity;
	private EventStatus status;
	private final List<Registration> registrations;

	private Event(EventId id, NgoId ownerId, EventTitle title, Description description, Location location,
			Period period, Capacity capacity, EventStatus status, List<Registration> registrations) {
		super(id);
		this.ownerId = Guard.notNull(ownerId, "ONG organizadora é obrigatória");
		this.title = Guard.notNull(title, "Título é obrigatório");
		this.description = Guard.notNull(description, "Descrição é obrigatória");
		this.location = Guard.notNull(location, "Local é obrigatório");
		this.period = Guard.notNull(period, "Período é obrigatório");
		this.capacity = Guard.notNull(capacity, "Capacidade é obrigatória");
		this.status = Guard.notNull(status, "Status é obrigatório");
		this.registrations = new ArrayList<>(registrations);
	}

	public static Event create(NgoId ownerId, EventTitle title, Description description, Location location,
			Period period, Capacity capacity, Instant now) {
		Guard.notNull(period, "Período é obrigatório");
		Guard.check(!period.hasStarted(now), "O evento deve começar no futuro");
		Event event = new Event(EventId.generate(), ownerId, title, description, location, period, capacity,
				EventStatus.DRAFT, List.of());
		event.raise(new EventCreated(event.id(), ownerId));
		return event;
	}

	public static Event restore(EventId id, NgoId ownerId, EventTitle title, Description description,
			Location location, Period period, Capacity capacity, EventStatus status,
			List<Registration> registrations) {
		return new Event(id, ownerId, title, description, location, period, capacity, status, registrations);
	}

	public void publish(Instant now) {
		Guard.check(status == EventStatus.DRAFT, "Somente rascunhos podem ser publicados");
		Guard.check(!period.hasStarted(now), "O evento deve começar no futuro");
		status = EventStatus.PUBLISHED;
		raise(new EventPublished(id(), ownerId));
	}

	public void cancel() {
		ensureOpenForChanges();
		List<VolunteerId> affected = activeVolunteers();
		status = EventStatus.CANCELLED;
		raise(new EventCancelled(id(), affected));
	}

	public void finish(Instant now) {
		Guard.check(status == EventStatus.PUBLISHED, "Somente eventos publicados podem ser encerrados");
		Guard.check(period.hasEnded(now), "O evento ainda não terminou");
		status = EventStatus.FINISHED;
		raise(new EventFinished(id()));
	}

	public void updateDetails(EventTitle title, Description description, Location location) {
		ensureOpenForChanges();
		this.title = Guard.notNull(title, "Título é obrigatório");
		this.description = Guard.notNull(description, "Descrição é obrigatória");
		this.location = Guard.notNull(location, "Local é obrigatório");
	}

	public void reschedule(Period newPeriod, Instant now) {
		ensureOpenForChanges();
		Guard.notNull(newPeriod, "Período é obrigatório");
		Guard.check(!newPeriod.hasStarted(now), "O evento deve começar no futuro");
		this.period = newPeriod;
		raise(new EventRescheduled(id(), newPeriod, activeVolunteers()));
	}

	public void changeCapacity(Capacity newCapacity) {
		ensureOpenForChanges();
		Guard.notNull(newCapacity, "Capacidade é obrigatória");
		Guard.check(newCapacity.max() >= occupiedSpots(),
				"Capacidade não pode ser menor que o número de inscritos");
		this.capacity = newCapacity;
	}

	public void register(VolunteerId volunteerId, Instant now) {
		Guard.notNull(volunteerId, "Voluntário é obrigatório");
		Guard.check(status == EventStatus.PUBLISHED, "O evento não está aberto para inscrições");
		Guard.check(!period.hasStarted(now), "As inscrições estão encerradas");
		Guard.check(findActive(volunteerId) == null, "Voluntário já está inscrito");
		Guard.check(occupiedSpots() < capacity.max(), "Evento lotado");
		registrations.add(Registration.confirm(volunteerId, now));
		raise(new VolunteerRegistered(id(), ownerId, volunteerId));
	}

	public void cancelRegistration(VolunteerId volunteerId, Instant now) {
		Registration registration = requireActive(volunteerId);
		Guard.check(!period.hasStarted(now), "O evento já começou");
		registration.cancel();
		raise(new RegistrationCancelled(id(), ownerId, volunteerId));
	}

	public void recordAttendance(VolunteerId volunteerId, Instant now) {
		Guard.check(status == EventStatus.PUBLISHED, "O evento não está em andamento");
		Guard.check(period.hasStarted(now), "O evento ainda não começou");
		requireActive(volunteerId).attend();
		raise(new AttendanceRecorded(id(), volunteerId));
	}

	public boolean isRegistered(VolunteerId volunteerId) {
		return findActive(volunteerId) != null;
	}

	public int occupiedSpots() {
		return (int) registrations.stream().filter(Registration::isActive).count();
	}

	public int availableSpots() {
		return capacity.max() - occupiedSpots();
	}

	private Registration requireActive(VolunteerId volunteerId) {
		Registration registration = findActive(volunteerId);
		Guard.check(registration != null, "Voluntário não está inscrito");
		return registration;
	}

	private Registration findActive(VolunteerId volunteerId) {
		return registrations.stream()
				.filter(r -> r.isActive() && r.volunteerId().equals(volunteerId))
				.findFirst()
				.orElse(null);
	}

	private List<VolunteerId> activeVolunteers() {
		return registrations.stream().filter(Registration::isActive).map(Registration::volunteerId).toList();
	}

	private void ensureOpenForChanges() {
		Guard.check(status == EventStatus.DRAFT || status == EventStatus.PUBLISHED,
				"Evento cancelado ou encerrado não pode ser alterado");
	}

	public NgoId ownerId() {
		return ownerId;
	}

	public EventTitle title() {
		return title;
	}

	public Description description() {
		return description;
	}

	public Location location() {
		return location;
	}

	public Period period() {
		return period;
	}

	public Capacity capacity() {
		return capacity;
	}

	public EventStatus status() {
		return status;
	}

	public List<Registration> registrations() {
		return Collections.unmodifiableList(registrations);
	}
}
