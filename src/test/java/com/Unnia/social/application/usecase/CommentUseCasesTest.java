package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.CommentView;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.domain.event.CommentDeleted;
import com.Unnia.social.domain.event.CommentPosted;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentUseCasesTest {

	private SocialFixture fx;
	private AccountId author;
	private AccountId commenter;
	private PostId postId;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		author = fx.account();
		commenter = fx.account();
		postId = fx.post(author, "Post");
	}

	private AddCommentUseCase add() {
		return new AddCommentUseCase(fx.posts, fx.comments, fx.publisher, fx.clock);
	}

	private ReplyToCommentUseCase reply() {
		return new ReplyToCommentUseCase(fx.posts, fx.comments, fx.publisher, fx.clock);
	}

	@Test
	void commentsOnPostAndPublishesEvent() {
		CommentId id = add().execute(new AddCommentUseCase.Command(commenter, postId, "Parabéns!"));

		Comment comment = fx.comments.findById(id).orElseThrow();
		assertThat(comment.postId()).isEqualTo(postId);
		assertThat(comment.authorId()).isEqualTo(commenter);
		assertThat(fx.publisher.published()).containsExactly(new CommentPosted(id, postId, commenter));
	}

	@Test
	void cannotCommentOnMissingOrDeletedPostOrWithBlankText() {
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(author, postId));

		assertThatThrownBy(() -> add().execute(new AddCommentUseCase.Command(commenter, postId, "oi")))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> add().execute(new AddCommentUseCase.Command(commenter, PostId.generate(), "oi")))
				.isInstanceOf(NotFoundException.class);
		PostId other = fx.post(author, "Outro");
		assertThatThrownBy(() -> add().execute(new AddCommentUseCase.Command(commenter, other, " ")))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void repliesToACommentKeepingTheSamePost() {
		CommentId parent = fx.comment(commenter, postId, "Pergunta");

		CommentId replyId = reply().execute(new ReplyToCommentUseCase.Command(author, parent, "Resposta"));

		Comment reply = fx.comments.findById(replyId).orElseThrow();
		assertThat(reply.postId()).isEqualTo(postId);
		assertThat(reply.parentId()).contains(parent);
	}

	@Test
	void repliesHaveASingleLevelAndRequireAnActiveParent() {
		CommentId parent = fx.comment(commenter, postId, "Pergunta");
		CommentId replyId = reply().execute(new ReplyToCommentUseCase.Command(author, parent, "Resposta"));

		assertThatThrownBy(() -> reply().execute(new ReplyToCommentUseCase.Command(commenter, replyId, "x")))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> reply().execute(
				new ReplyToCommentUseCase.Command(commenter, CommentId.generate(), "x")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void cannotReplyWhenThePostWasDeleted() {
		CommentId parent = fx.comment(commenter, postId, "Pergunta");
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(author, postId));

		assertThatThrownBy(() -> reply().execute(new ReplyToCommentUseCase.Command(author, parent, "x")))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void onlyTheAuthorDeletesTheComment() {
		CommentId id = fx.comment(commenter, postId, "Meu comentário");
		DeleteCommentUseCase useCase = new DeleteCommentUseCase(fx.comments, fx.publisher);

		assertThatThrownBy(() -> useCase.execute(new DeleteCommentUseCase.Command(author, id)))
				.isInstanceOf(ForbiddenException.class);
		useCase.execute(new DeleteCommentUseCase.Command(commenter, id));

		assertThat(fx.publisher.published()).containsExactly(new CommentDeleted(id));
		assertThatThrownBy(() -> useCase.execute(new DeleteCommentUseCase.Command(commenter, id)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void listsActiveCommentsWithNestedReplies() {
		CommentId first = fx.comment(commenter, postId, "Primeiro");
		CommentId second = fx.comment(author, postId, "Segundo");
		CommentId deleted = fx.comment(commenter, postId, "Apagado");
		reply().execute(new ReplyToCommentUseCase.Command(author, first, "Resposta ao primeiro"));
		new DeleteCommentUseCase(fx.comments, fx.publisher)
				.execute(new DeleteCommentUseCase.Command(commenter, deleted));

		List<CommentView> views = new ListCommentsUseCase(fx.posts, fx.comments).execute(postId);

		assertThat(views).extracting(CommentView::commentId).containsExactly(first, second);
		assertThat(views.get(0).replies()).extracting(CommentView::text).containsExactly("Resposta ao primeiro");
		assertThat(views.get(1).replies()).isEmpty();
	}

	@Test
	void listingFailsForDeletedPost() {
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(author, postId));

		assertThatThrownBy(() -> new ListCommentsUseCase(fx.posts, fx.comments).execute(postId))
				.isInstanceOf(NotFoundException.class);
	}
}
