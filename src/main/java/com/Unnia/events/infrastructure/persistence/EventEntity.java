package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.EventStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O {@code version} implementa o bloqueio otimista: duas inscrições simultâneas no mesmo evento
 * não conseguem, juntas, ultrapassar as vagas.
 */
@Entity
@Table(name = "events")
class EventEntity {

	@Id
	UUID id;

	@Column(name = "owner_id", nullable = false)
	UUID ownerId;

	@Column(nullable = false, length = 150)
	String title;

	@Column(nullable = false, length = 2000)
	String description;

	@Column(nullable = false, length = 200)
	String address;

	@Column(nullable = false, length = 80)
	String city;

	@Column(name = "starts_at", nullable = false)
	Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	Instant endsAt;

	@Column(nullable = false)
	int capacity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	EventStatus status;

	@Version
	Long version;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "event_id", nullable = false)
	List<RegistrationEntity> registrations = new ArrayList<>();

	protected EventEntity() {
	}
}
