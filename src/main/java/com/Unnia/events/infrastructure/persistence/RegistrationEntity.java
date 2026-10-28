package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_registrations")
class RegistrationEntity {

	@Id
	UUID id;

	@Column(name = "volunteer_id", nullable = false)
	UUID volunteerId;

	@Column(name = "registered_at", nullable = false)
	Instant registeredAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	RegistrationStatus status;

	protected RegistrationEntity() {
	}
}
