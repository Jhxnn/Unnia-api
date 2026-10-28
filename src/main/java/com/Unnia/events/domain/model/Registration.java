package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.Entity;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.VolunteerId;

import java.time.Instant;

/**
 * Entidade interna do agregado {@link Event}. Só o Event cria e altera inscrições,
 * para que o invariante de vagas seja sempre respeitado.
 */
public class Registration extends Entity<RegistrationId> {

	private final VolunteerId volunteerId;
	private final Instant registeredAt;
	private RegistrationStatus status;

	private Registration(RegistrationId id, VolunteerId volunteerId, Instant registeredAt,
			RegistrationStatus status) {
		super(id);
		this.volunteerId = Guard.notNull(volunteerId, "Voluntário é obrigatório");
		this.registeredAt = Guard.notNull(registeredAt, "Data da inscrição é obrigatória");
		this.status = Guard.notNull(status, "Status é obrigatório");
	}

	static Registration confirm(VolunteerId volunteerId, Instant now) {
		return new Registration(RegistrationId.generate(), volunteerId, now, RegistrationStatus.CONFIRMED);
	}

	public static Registration restore(RegistrationId id, VolunteerId volunteerId, Instant registeredAt,
			RegistrationStatus status) {
		return new Registration(id, volunteerId, registeredAt, status);
	}

	void cancel() {
		Guard.check(status == RegistrationStatus.CONFIRMED, "Somente inscrições confirmadas podem ser canceladas");
		status = RegistrationStatus.CANCELLED;
	}

	void attend() {
		Guard.check(status == RegistrationStatus.CONFIRMED, "Somente inscrições confirmadas podem ter presença");
		status = RegistrationStatus.ATTENDED;
	}

	public boolean isActive() {
		return status != RegistrationStatus.CANCELLED;
	}

	public VolunteerId volunteerId() {
		return volunteerId;
	}

	public Instant registeredAt() {
		return registeredAt;
	}

	public RegistrationStatus status() {
		return status;
	}
}
