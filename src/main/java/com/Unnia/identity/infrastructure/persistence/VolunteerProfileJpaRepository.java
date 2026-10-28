package com.Unnia.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface VolunteerProfileJpaRepository extends JpaRepository<VolunteerProfileEntity, UUID> {

	Optional<VolunteerProfileEntity> findByAccountId(UUID accountId);
}
