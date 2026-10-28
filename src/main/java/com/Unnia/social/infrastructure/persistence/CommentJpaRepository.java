package com.Unnia.social.infrastructure.persistence;

import com.Unnia.social.domain.model.ContentStatus;
import com.Unnia.social.infrastructure.persistence.SocialEntities.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CommentJpaRepository extends JpaRepository<CommentEntity, UUID> {

	List<CommentEntity> findByPostIdOrderByCreatedAtAsc(UUID postId);

	long countByPostIdAndStatus(UUID postId, ContentStatus status);
}
