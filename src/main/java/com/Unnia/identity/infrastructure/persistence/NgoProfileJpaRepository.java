package com.Unnia.identity.infrastructure.persistence;

import com.Unnia.identity.domain.model.VerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface NgoProfileJpaRepository extends JpaRepository<NgoProfileEntity, UUID> {

	Optional<NgoProfileEntity> findByAccountId(UUID accountId);

	boolean existsByCnpj(String cnpj);

	List<NgoProfileEntity> findByVerificationStatusOrderByName(VerificationStatus status, Pageable pageable);
}
