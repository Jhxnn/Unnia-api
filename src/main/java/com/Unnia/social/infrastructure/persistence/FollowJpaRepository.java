package com.Unnia.social.infrastructure.persistence;

import com.Unnia.social.infrastructure.persistence.SocialEntities.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FollowJpaRepository extends JpaRepository<FollowEntity, UUID> {

	Optional<FollowEntity> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

	List<FollowEntity> findByFollowerId(UUID followerId);

	long countByFolloweeId(UUID followeeId);
}
