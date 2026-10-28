package com.Unnia.messaging.infrastructure.persistence;

import com.Unnia.messaging.infrastructure.persistence.MessagingEntities.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

	Optional<ConversationEntity> findByParticipantAAndParticipantB(UUID participantA, UUID participantB);

	List<ConversationEntity> findByParticipantAOrParticipantB(UUID participantA, UUID participantB);
}
