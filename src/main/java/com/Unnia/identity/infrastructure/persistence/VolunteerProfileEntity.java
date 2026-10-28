package com.Unnia.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "volunteer_profiles")
class VolunteerProfileEntity {

	@Id
	UUID id;

	@Column(name = "account_id", nullable = false, unique = true)
	UUID accountId;

	@Column(nullable = false, length = 120)
	String name;

	@Column(nullable = false, length = 2000)
	String bio;

	@Column(nullable = false, length = 80)
	String city;

	protected VolunteerProfileEntity() {
	}
}
