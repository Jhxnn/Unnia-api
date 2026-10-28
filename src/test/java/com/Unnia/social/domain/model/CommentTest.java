package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.event.CommentDeleted;
import com.Unnia.social.domain.event.CommentPosted;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final AccountId AUTHOR = AccountId.generate();
	private static final AccountId COMMENTER = AccountId.generate();

	private Post post() {
		return Post.publish(AUTHOR, PostContent.of("Post"), List.of(), null, NOW);
	}

	@Test
	void commentsOnActivePostAndRaisesEvent() {
		Post post = post();

		Comment comment = Comment.onPost(post, COMMENTER, CommentContent.of("Parabéns!"), NOW);

		assertThat(comment.postId()).isEqualTo(post.id());
		assertThat(comment.authorId()).isEqualTo(COMMENTER);
		assertThat(comment.isReply()).isFalse();
		assertThat(comment.parentId()).isEmpty();
		assertThat(comment.pullDomainEvents())
				.containsExactly(new CommentPosted(comment.id(), post.id(), COMMENTER));
	}

	@Test
	void cannotCommentOnDeletedPost() {
		Post post = post();
		post.delete(AUTHOR);

		assertThatThrownBy(() -> Comment.onPost(post, COMMENTER, CommentContent.of("oi"), NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void repliesBelongToTheSamePost() {
		Post post = post();
		Comment parent = Comment.onPost(post, COMMENTER, CommentContent.of("Pergunta"), NOW);

		Comment reply = Comment.replyTo(parent, AUTHOR, CommentContent.of("Resposta"), NOW);

		assertThat(reply.postId()).isEqualTo(post.id());
		assertThat(reply.isReply()).isTrue();
		assertThat(reply.parentId()).contains(parent.id());
	}

	@Test
	void repliesHaveASingleLevel() {
		Comment parent = Comment.onPost(post(), COMMENTER, CommentContent.of("a"), NOW);
		Comment reply = Comment.replyTo(parent, AUTHOR, CommentContent.of("b"), NOW);

		assertThatThrownBy(() -> Comment.replyTo(reply, COMMENTER, CommentContent.of("c"), NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void cannotReplyToDeletedComment() {
		Comment parent = Comment.onPost(post(), COMMENTER, CommentContent.of("a"), NOW);
		parent.delete(COMMENTER);

		assertThatThrownBy(() -> Comment.replyTo(parent, AUTHOR, CommentContent.of("b"), NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void onlyAuthorCanDeleteAndOnlyOnce() {
		Comment comment = Comment.onPost(post(), COMMENTER, CommentContent.of("a"), NOW);
		comment.pullDomainEvents();

		assertThatThrownBy(() -> comment.delete(AUTHOR)).isInstanceOf(DomainException.class);
		comment.delete(COMMENTER);

		assertThat(comment.isActive()).isFalse();
		assertThat(comment.pullDomainEvents()).containsExactly(new CommentDeleted(comment.id()));
		assertThatThrownBy(() -> comment.delete(COMMENTER)).isInstanceOf(DomainException.class);
	}

	@Test
	void contentIsRequiredAndLimited() {
		assertThatThrownBy(() -> CommentContent.of("")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> CommentContent.of("x".repeat(1001))).isInstanceOf(DomainException.class);
	}
}
