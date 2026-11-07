package com.Unnia.social.infrastructure.persistence;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentContent;
import com.Unnia.social.domain.model.ContentStatus;
import com.Unnia.social.domain.model.Follow;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostContent;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import com.Unnia.social.domain.model.TargetType;
import com.Unnia.social.infrastructure.persistence.SocialEntities.CommentEntity;
import com.Unnia.social.infrastructure.persistence.SocialEntities.PostEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SocialMapperTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final AccountId AUTHOR = AccountId.generate();

	@Test
	void postKeepsMediaOrderLinkedEventAndEditTime() {
		EventId eventId = EventId.generate();
		Post post = Post.publish(AUTHOR, PostContent.of("Texto"),
				List.of(MediaRef.image("https://x.com/1.png"), MediaRef.video("https://x.com/2.mp4")), eventId, NOW);
		post.edit(AUTHOR, PostContent.of("Editado"), NOW.plusSeconds(60));

		PostEntity entity = SocialMapper.toEntity(post);
		Post restored = SocialMapper.toDomain(entity);

		assertThat(entity.media).extracting(m -> m.url).containsExactly("https://x.com/1.png", "https://x.com/2.mp4");
		assertThat(restored).isEqualTo(post);
		assertThat(restored.content()).isEqualTo(PostContent.of("Editado"));
		assertThat(restored.media()).isEqualTo(post.media());
		assertThat(restored.linkedEventId()).contains(eventId);
		assertThat(restored.editedAt()).contains(NOW.plusSeconds(60));
		assertThat(restored.domainEvents()).isEmpty();
	}

	@Test
	void postWithoutOptionalFieldsRoundTrips() {
		Post post = Post.publish(AUTHOR, PostContent.of("Só texto"), List.of(), null, NOW);

		Post restored = SocialMapper.toDomain(SocialMapper.toEntity(post));

		assertThat(restored.linkedEventId()).isEmpty();
		assertThat(restored.editedAt()).isEmpty();
		assertThat(restored.media()).isEmpty();
	}

	@Test
	void deletedPostStaysDeleted() {
		Post post = Post.publish(AUTHOR, PostContent.of("x"), List.of(), null, NOW);
		post.delete(AUTHOR);

		Post restored = SocialMapper.toDomain(SocialMapper.toEntity(post));

		assertThat(restored.status()).isEqualTo(ContentStatus.DELETED);
		assertThat(restored.isActive()).isFalse();
	}

	@Test
	void commentAndReplyKeepTheirParentLink() {
		Post post = Post.publish(AUTHOR, PostContent.of("x"), List.of(), null, NOW);
		Comment root = Comment.onPost(post, AccountId.generate(), CommentContent.of("a"), NOW);
		Comment reply = Comment.replyTo(root, AUTHOR, CommentContent.of("b"), NOW);

		CommentEntity rootEntity = SocialMapper.toEntity(root);
		CommentEntity replyEntity = SocialMapper.toEntity(reply);

		assertThat(rootEntity.parentId).isNull();
		assertThat(replyEntity.parentId).isEqualTo(root.id().value());
		assertThat(SocialMapper.toDomain(rootEntity).isReply()).isFalse();
		assertThat(SocialMapper.toDomain(replyEntity).parentId()).contains(root.id());
		assertThat(SocialMapper.toDomain(replyEntity).postId()).isEqualTo(post.id());
	}

	@Test
	void reactionKeepsTargetAndType() {
		ReactionTarget target = ReactionTarget.post(PostId.generate());
		Reaction reaction = Reaction.react(AUTHOR, target, ReactionType.SUPPORT, NOW);

		var entity = SocialMapper.toEntity(reaction);
		Reaction restored = SocialMapper.toDomain(entity);

		assertThat(entity.targetType).isEqualTo(TargetType.POST);
		assertThat(entity.targetId).isEqualTo(target.id());
		assertThat(restored.target()).isEqualTo(target);
		assertThat(restored.type()).isEqualTo(ReactionType.SUPPORT);
	}

	@Test
	void followKeepsBothSides() {
		AccountId followee = AccountId.generate();
		Follow follow = Follow.create(AUTHOR, followee, NOW);

		Follow restored = SocialMapper.toDomain(SocialMapper.toEntity(follow));

		assertThat(restored.followerId()).isEqualTo(AUTHOR);
		assertThat(restored.followeeId()).isEqualTo(followee);
		assertThat(restored.since()).isEqualTo(NOW);
	}
}
