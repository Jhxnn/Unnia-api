package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "ngo_profiles")
class NgoProfileEntity {

	@Id
	UUID id;

	@Column(name = "account_id", nullable = false, unique = true)
	UUID accountId;

	@Column(nullable = false, unique = true, length = 14)
	String cnpj;

	@Column(nullable = false, length = 120)
	String name;

	@Column(nullable = false, length = 2000)
	String description;

	@Column(nullable = false, length = 80)
	String city;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 20)
	VerificationStatus verificationStatus;

	protected NgoProfileEntity() {
	}
}
