package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.domain.event.CommentDeleted;
import com.Unnia.social.domain.event.PostDeleted;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModeratorRemovalUseCasesTest {

	private SocialFixture fx;
	private AccountId author;
	private PostId postId;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		author = fx.account();
		postId = fx.post(author, "Conteúdo problemático");
	}

	@Test
	void removesAPostWithoutBeingItsAuthor() {
		new RemovePostByModeratorUseCase(fx.posts, fx.publisher).execute(postId);

		assertThat(fx.posts.findById(postId).orElseThrow().isActive()).isFalse();
		assertThat(fx.publisher.published()).containsExactly(new PostDeleted(postId));
	}

	@Test
	void removingAnAlreadyRemovedPostIsANoOp() {
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(author, postId));
		fx.publisher.clear();

		new RemovePostByModeratorUseCase(fx.posts, fx.publisher).execute(postId);

		assertThat(fx.publisher.published()).isEmpty();
	}

	@Test
	void removesACommentWithoutBeingItsAuthor() {
		CommentId commentId = fx.comment(fx.account(), postId, "Ofensa");

		new RemoveCommentByModeratorUseCase(fx.comments, fx.publisher).execute(commentId);

		assertThat(fx.comments.findById(commentId).orElseThrow().isActive()).isFalse();
		assertThat(fx.publisher.published()).containsExactly(new CommentDeleted(commentId));
		fx.publisher.clear();
		new RemoveCommentByModeratorUseCase(fx.comments, fx.publisher).execute(commentId);
		assertThat(fx.publisher.published()).isEmpty();
	}

	@Test
	void unknownContentIsNotFound() {
		assertThatThrownBy(() -> new RemovePostByModeratorUseCase(fx.posts, fx.publisher).execute(PostId.generate()))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> new RemoveCommentByModeratorUseCase(fx.comments, fx.publisher)
				.execute(CommentId.generate())).isInstanceOf(NotFoundException.class);
	}
}
