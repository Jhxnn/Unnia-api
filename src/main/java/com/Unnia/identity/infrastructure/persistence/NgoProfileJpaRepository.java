package com.Unnia.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface NgoProfileJpaRepository extends JpaRepository<NgoProfileEntity, UUID> {

	Optional<NgoProfileEntity> findByAccountId(UUID accountId);

	boolean existsByCnpj(String cnpj);
}
