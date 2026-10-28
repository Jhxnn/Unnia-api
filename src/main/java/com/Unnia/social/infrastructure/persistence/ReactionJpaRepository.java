package com.Unnia.social.infrastructure.persistence;

import com.Unnia.social.domain.model.TargetType;
import com.Unnia.social.infrastructure.persistence.SocialEntities.ReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ReactionJpaRepository extends JpaRepository<ReactionEntity, UUID> {

	Optional<ReactionEntity> findByAuthorIdAndTargetTypeAndTargetId(UUID authorId, TargetType targetType,
			UUID targetId);

	long countByTargetTypeAndTargetId(TargetType targetType, UUID targetId);
}
