package com.Unnia.social.infrastructure.persistence;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.ContentStatus;
import com.Unnia.social.domain.model.Follow;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.FollowRepository;
import com.Unnia.social.domain.repository.PostRepository;
import com.Unnia.social.domain.repository.ReactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adaptadores dos repositórios do domínio social sobre Spring Data JPA. */
final class JpaSocialRepositories {

	private JpaSocialRepositories() {
	}

	@Repository
	static class JpaPostRepository implements PostRepository {

		private final PostJpaRepository jpa;

		JpaPostRepository(PostJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Post save(Post post) {
			jpa.save(SocialMapper.toEntity(post));
			return post;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Post> findById(PostId id) {
			return jpa.findById(id.value()).map(SocialMapper::toDomain);
		}

		@Override
		@Transactional(readOnly = true)
		public List<Post> findByAuthor(AccountId authorId) {
			return jpa.findByAuthorId(authorId.value()).stream().map(SocialMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public List<Post> findFeed(List<AccountId> authors, int page, int size) {
			return jpa.findByAuthorIdInAndStatusOrderByCreatedAtDesc(authors.stream().map(AccountId::value).toList(),
					ContentStatus.ACTIVE, PageRequest.of(page, size)).stream().map(SocialMapper::toDomain).toList();
		}
	}

	@Repository
	static class JpaCommentRepository implements CommentRepository {

		private final CommentJpaRepository jpa;

		JpaCommentRepository(CommentJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Comment save(Comment comment) {
			jpa.save(SocialMapper.toEntity(comment));
			return comment;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Comment> findById(CommentId id) {
			return jpa.findById(id.value()).map(SocialMapper::toDomain);
		}

		@Override
		@Transactional(readOnly = true)
		public List<Comment> findByPost(PostId postId) {
			return jpa.findByPostIdOrderByCreatedAtAsc(postId.value()).stream().map(SocialMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public long countActiveByPost(PostId postId) {
			return jpa.countByPostIdAndStatus(postId.value(), ContentStatus.ACTIVE);
		}
	}

	@Repository
	static class JpaReactionRepository implements ReactionRepository {

		private final ReactionJpaRepository jpa;

		JpaReactionRepository(ReactionJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Reaction save(Reaction reaction) {
			jpa.save(SocialMapper.toEntity(reaction));
			return reaction;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Reaction> findByAuthorAndTarget(AccountId authorId, ReactionTarget target) {
			return jpa.findByAuthorIdAndTargetTypeAndTargetId(authorId.value(), target.type(), target.id())
					.map(SocialMapper::toDomain);
		}

		@Override
		public void delete(Reaction reaction) {
			jpa.deleteById(reaction.id().value());
		}

		@Override
		@Transactional(readOnly = true)
		public long countByTarget(ReactionTarget target) {
			return jpa.countByTargetTypeAndTargetId(target.type(), target.id());
		}
	}

	@Repository
	static class JpaFollowRepository implements FollowRepository {

		private final FollowJpaRepository jpa;

		JpaFollowRepository(FollowJpaRepository jpa) {
			this.jpa = jpa;
		}

		@Override
		public Follow save(Follow follow) {
			jpa.save(SocialMapper.toEntity(follow));
			return follow;
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Follow> findByFollowerAndFollowee(AccountId followerId, AccountId followeeId) {
			return jpa.findByFollowerIdAndFolloweeId(followerId.value(), followeeId.value())
					.map(SocialMapper::toDomain);
		}

		@Override
		public void delete(Follow follow) {
			jpa.deleteById(follow.id().value());
		}

		@Override
		@Transactional(readOnly = true)
		public List<Follow> findByFollower(AccountId followerId) {
			return jpa.findByFollowerId(followerId.value()).stream().map(SocialMapper::toDomain).toList();
		}

		@Override
		@Transactional(readOnly = true)
		public long countByFollowee(AccountId followeeId) {
			return jpa.countByFolloweeId(followeeId.value());
		}
	}
}
