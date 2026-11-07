package com.Unnia.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

	Optional<EmailVerificationTokenEntity> findFirstByAccountIdOrderByIssuedAtDesc(UUID accountId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update EmailVerificationTokenEntity t set t.usedAt = :now where t.accountId = :accountId and t.usedAt is null")
	int invalidateAllOf(@Param("accountId") UUID accountId, @Param("now") Instant now);
}
