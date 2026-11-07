package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.CommentView;
import com.Unnia.social.application.PostView;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.application.port.AuthorDirectory;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** O que a tela precisa para mostrar quem escreveu, o que eu já reagi e quem eu sigo. */
class AuthorsAndFollowStatusTest {

	private SocialFixture fx;
	private AccountId ngo;
	private AccountId volunteer;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		ngo = fx.directories.newAccount("Mãos Solidárias", "NGO");
		volunteer = fx.directories.newAccount("Maria Souza", "VOLUNTEER");
	}

	private void react(AccountId who, PostId postId, ReactionType type) {
		new ReactUseCase(fx.posts, fx.comments, fx.reactions, fx.publisher, fx.clock)
				.execute(new ReactUseCase.Command(who, ReactionTarget.post(postId), type));
	}

	@Test
	void postsCarryTheirAuthorNameAndRole() {
		fx.post(ngo, "Mutirão sábado");

		List<PostView> feed = new GetFeedUseCase(fx.follows, fx.posts, fx.views).execute(ngo, 0, 10);

		assertThat(feed).singleElement().satisfies(view -> {
			assertThat(view.author().name()).isEqualTo("Mãos Solidárias");
			assertThat(view.author().role()).isEqualTo("NGO");
			assertThat(view.author().profileId()).isNotNull();
		});
	}

	@Test
	void anAuthorWhoNoLongerExistsIsShownAsRemovedInsteadOfBreakingTheFeed() {
		AccountId gone = fx.account();
		fx.post(gone, "Post de conta removida");

		PostView view = new GetFeedUseCase(fx.follows, fx.posts, fx.views).execute(gone, 0, 10).get(0);

		assertThat(view.author()).isEqualTo(AuthorDirectory.UNKNOWN);
	}

	@Test
	void postsShowMyReactionButNotSomeoneElses() {
		PostId postId = fx.post(ngo, "Post");
		react(volunteer, postId, ReactionType.LOVE);

		PostView mine = new GetPostUseCase(fx.posts, fx.views).execute(postId, volunteer);
		PostView author = new GetPostUseCase(fx.posts, fx.views).execute(postId, ngo);

		assertThat(mine.myReaction()).isEqualTo(ReactionType.LOVE);
		assertThat(mine.reactionCount()).isEqualTo(1);
		assertThat(author.myReaction()).isNull();
	}

	@Test
	void commentsAndRepliesCarryTheirAuthors() {
		PostId postId = fx.post(ngo, "Post");
		var first = fx.comment(volunteer, postId, "Posso ajudar?");
		new ReplyToCommentUseCase(fx.posts, fx.comments, fx.publisher, fx.clock)
				.execute(new ReplyToCommentUseCase.Command(ngo, first, "Pode sim!"));

		List<CommentView> comments = new ListCommentsUseCase(fx.posts, fx.comments, fx.directories).execute(postId);

		assertThat(comments.get(0).author().name()).isEqualTo("Maria Souza");
		assertThat(comments.get(0).replies().get(0).author().name()).isEqualTo("Mãos Solidárias");
	}

	@Test
	void followStatusSaysIfIFollowAndHowManyFollow() {
		GetFollowStatusUseCase status = new GetFollowStatusUseCase(fx.follows);
		assertThat(status.execute(volunteer, ngo)).isEqualTo(new GetFollowStatusUseCase.Status(false, 0));

		new FollowAccountUseCase(fx.directories, fx.follows, fx.publisher, fx.clock)
				.execute(new FollowAccountUseCase.Command(volunteer, ngo));

		assertThat(status.execute(volunteer, ngo)).isEqualTo(new GetFollowStatusUseCase.Status(true, 1));
		assertThat(status.execute(ngo, volunteer)).isEqualTo(new GetFollowStatusUseCase.Status(false, 0));
	}
}
