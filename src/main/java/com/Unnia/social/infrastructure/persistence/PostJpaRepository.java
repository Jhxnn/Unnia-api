package com.Unnia.social.infrastructure.persistence;

import com.Unnia.social.domain.model.ContentStatus;
import com.Unnia.social.infrastructure.persistence.SocialEntities.PostEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface PostJpaRepository extends JpaRepository<PostEntity, UUID> {

	List<PostEntity> findByAuthorId(UUID authorId);

	List<PostEntity> findByAuthorIdInAndStatusOrderByCreatedAtDesc(Collection<UUID> authors, ContentStatus status,
			Pageable pageable);
}
