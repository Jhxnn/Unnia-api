package com.Unnia.social.infrastructure.persistence;

import com.Unnia.social.domain.model.ContentStatus;
import com.Unnia.social.domain.model.MediaType;
import com.Unnia.social.domain.model.ReactionType;
import com.Unnia.social.domain.model.TargetType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Entidades JPA do contexto social; detalhe de infraestrutura, invisível para o domínio. */
final class SocialEntities {

	private SocialEntities() {
	}

	@Embeddable
	static class MediaEmbeddable {

		@Column(nullable = false, length = 500)
		String url;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 10)
		MediaType type;

		protected MediaEmbeddable() {
		}
	}

	@Entity
	@Table(name = "posts")
	static class PostEntity {

		@Id
		UUID id;

		@Column(name = "author_id", nullable = false)
		UUID authorId;

		@Column(nullable = false, length = 3000)
		String content;

		@Column(name = "linked_event_id")
		UUID linkedEventId;

		@Column(name = "created_at", nullable = false)
		Instant createdAt;

		@Column(name = "edited_at")
		Instant editedAt;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 20)
		ContentStatus status;

		@ElementCollection
		@CollectionTable(name = "post_media", joinColumns = @JoinColumn(name = "post_id"))
		@OrderColumn(name = "position")
		List<MediaEmbeddable> media = new ArrayList<>();

		protected PostEntity() {
		}
	}

	@Entity
	@Table(name = "comments")
	static class CommentEntity {

		@Id
		UUID id;

		@Column(name = "post_id", nullable = false)
		UUID postId;

		@Column(name = "parent_id")
		UUID parentId;

		@Column(name = "author_id", nullable = false)
		UUID authorId;

		@Column(nullable = false, length = 1000)
		String content;

		@Column(name = "created_at", nullable = false)
		Instant createdAt;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 20)
		ContentStatus status;

		protected CommentEntity() {
		}
	}

	@Entity
	@Table(name = "reactions")
	static class ReactionEntity {

		@Id
		UUID id;

		@Column(name = "author_id", nullable = false)
		UUID authorId;

		@Enumerated(EnumType.STRING)
		@Column(name = "target_type", nullable = false, length = 10)
		TargetType targetType;

		@Column(name = "target_id", nullable = false)
		UUID targetId;

		@Enumerated(EnumType.STRING)
		@Column(nullable = false, length = 10)
		ReactionType type;

		@Column(name = "created_at", nullable = false)
		Instant createdAt;

		protected ReactionEntity() {
		}
	}

	@Entity
	@Table(name = "follows")
	static class FollowEntity {

		@Id
		UUID id;

		@Column(name = "follower_id", nullable = false)
		UUID followerId;

		@Column(name = "followee_id", nullable = false)
		UUID followeeId;

		@Column(nullable = false)
		Instant since;

		protected FollowEntity() {
		}
	}
}
