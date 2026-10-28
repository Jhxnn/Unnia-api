package com.Unnia.events.infrastructure.persistence;

import com.Unnia.events.domain.model.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

	List<EventEntity> findByOwnerIdOrderByStartsAtDesc(UUID ownerId);

	@Query("""
			select distinct e from EventEntity e join e.registrations r
			where r.volunteerId = :volunteerId
			  and r.status <> com.Unnia.events.domain.model.RegistrationStatus.CANCELLED
			order by e.startsAt desc""")
	List<EventEntity> findByActiveRegistration(@Param("volunteerId") UUID volunteerId);

	List<EventEntity> findByStatusAndStartsAtAfterOrderByStartsAtAsc(EventStatus status, Instant after,
			Pageable pageable);
}
