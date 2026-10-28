package com.Unnia.social.infrastructure.persistence;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentContent;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Follow;
import com.Unnia.social.domain.model.FollowId;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostContent;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.infrastructure.persistence.SocialEntities.CommentEntity;
import com.Unnia.social.infrastructure.persistence.SocialEntities.FollowEntity;
import com.Unnia.social.infrastructure.persistence.SocialEntities.MediaEmbeddable;
import com.Unnia.social.infrastructure.persistence.SocialEntities.PostEntity;
import com.Unnia.social.infrastructure.persistence.SocialEntities.ReactionEntity;

final class SocialMapper {

	private SocialMapper() {
	}

	static PostEntity toEntity(Post post) {
		PostEntity entity = new PostEntity();
		entity.id = post.id().value();
		entity.authorId = post.authorId().value();
		entity.content = post.content().text();
		entity.linkedEventId = post.linkedEventId().map(EventId::value).orElse(null);
		entity.createdAt = post.createdAt();
		entity.editedAt = post.editedAt().orElse(null);
		entity.status = post.status();
		for (MediaRef ref : post.media()) {
			MediaEmbeddable media = new MediaEmbeddable();
			media.url = ref.url();
			media.type = ref.type();
			entity.media.add(media);
		}
		return entity;
	}

	static Post toDomain(PostEntity entity) {
		return Post.restore(new PostId(entity.id), new AccountId(entity.authorId), new PostContent(entity.content),
				entity.media.stream().map(m -> new MediaRef(m.url, m.type)).toList(),
				entity.linkedEventId == null ? null : new EventId(entity.linkedEventId), entity.createdAt,
				entity.editedAt, entity.status);
	}

	static CommentEntity toEntity(Comment comment) {
		CommentEntity entity = new CommentEntity();
		entity.id = comment.id().value();
		entity.postId = comment.postId().value();
		entity.parentId = comment.parentId().map(CommentId::value).orElse(null);
		entity.authorId = comment.authorId().value();
		entity.content = comment.content().text();
		entity.createdAt = comment.createdAt();
		entity.status = comment.status();
		return entity;
	}

	static Comment toDomain(CommentEntity entity) {
		return Comment.restore(new CommentId(entity.id), new PostId(entity.postId),
				entity.parentId == null ? null : new CommentId(entity.parentId), new AccountId(entity.authorId),
				new CommentContent(entity.content), entity.createdAt, entity.status);
	}

	static ReactionEntity toEntity(Reaction reaction) {
		ReactionEntity entity = new ReactionEntity();
		entity.id = reaction.id().value();
		entity.authorId = reaction.authorId().value();
		entity.targetType = reaction.target().type();
		entity.targetId = reaction.target().id();
		entity.type = reaction.type();
		entity.createdAt = reaction.createdAt();
		return entity;
	}

	static Reaction toDomain(ReactionEntity entity) {
		return Reaction.restore(new ReactionId(entity.id), new AccountId(entity.authorId),
				new ReactionTarget(entity.targetType, entity.targetId), entity.type, entity.createdAt);
	}

	static FollowEntity toEntity(Follow follow) {
		FollowEntity entity = new FollowEntity();
		entity.id = follow.id().value();
		entity.followerId = follow.followerId().value();
		entity.followeeId = follow.followeeId().value();
		entity.since = follow.since();
		return entity;
	}

	static Follow toDomain(FollowEntity entity) {
		return Follow.restore(new FollowId(entity.id), new AccountId(entity.followerId),
				new AccountId(entity.followeeId), entity.since);
	}
}
