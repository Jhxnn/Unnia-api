package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.PostView;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedAndQueriesTest {

	private SocialFixture fx;
	private AccountId me;
	private AccountId followed;
	private AccountId stranger;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		me = fx.account();
		followed = fx.account();
		stranger = fx.account();
		new FollowAccountUseCase(fx.directories, fx.follows, fx.publisher, fx.clock)
				.execute(new FollowAccountUseCase.Command(me, followed));
	}

	private GetFeedUseCase feed() {
		return new GetFeedUseCase(fx.follows, fx.posts, fx.views);
	}

	@Test
	void feedHasOwnAndFollowedPostsNewestFirstAndSkipsStrangers() {
		PostId first = fx.post(followed, "Do seguido, antigo");
		fx.post(stranger, "De desconhecido");
		PostId second = fx.post(me, "Meu");
		PostId third = fx.post(followed, "Do seguido, novo");

		List<PostView> posts = feed().execute(me, 0, 10);

		assertThat(posts).extracting(PostView::postId).containsExactly(third, second, first);
	}

	@Test
	void feedExcludesDeletedPostsAndPaginates() {
		PostId keep1 = fx.post(me, "1");
		PostId deleted = fx.post(me, "2");
		PostId keep2 = fx.post(me, "3");
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(me, deleted));

		assertThat(feed().execute(me, 0, 1)).extracting(PostView::postId).containsExactly(keep2);
		assertThat(feed().execute(me, 1, 1)).extracting(PostView::postId).containsExactly(keep1);
		assertThat(feed().execute(me, 2, 1)).isEmpty();
	}

	@Test
	void feedValidatesPaging() {
		assertThatThrownBy(() -> feed().execute(me, -1, 10)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> feed().execute(me, 0, 0)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> feed().execute(me, 0, GetFeedUseCase.MAX_PAGE_SIZE + 1))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void postViewCountsReactionsAndActiveComments() {
		PostId postId = fx.post(followed, "Post");
		fx.comment(me, postId, "a");
		fx.comment(stranger, postId, "b");
		new ReactUseCase(fx.posts, fx.comments, fx.reactions, fx.publisher, fx.clock)
				.execute(new ReactUseCase.Command(me, ReactionTarget.post(postId), ReactionType.LIKE));

		PostView view = new GetPostUseCase(fx.posts, fx.views).execute(postId);

		assertThat(view.reactionCount()).isEqualTo(1);
		assertThat(view.commentCount()).isEqualTo(2);
		assertThat(view.authorId()).isEqualTo(followed);
	}

	@Test
	void getPostFailsForUnknownOrDeletedPost() {
		PostId postId = fx.post(me, "Post");
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(me, postId));

		assertThatThrownBy(() -> new GetPostUseCase(fx.posts, fx.views).execute(postId))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> new GetPostUseCase(fx.posts, fx.views).execute(PostId.generate()))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void listsAnAuthorsActivePostsNewestFirst() {
		PostId older = fx.post(followed, "Antigo");
		PostId deleted = fx.post(followed, "Apagado");
		PostId newer = fx.post(followed, "Novo");
		fx.post(stranger, "De outro");
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(followed, deleted));

		List<PostView> posts = new ListPostsByAuthorUseCase(fx.posts, fx.views).execute(followed);

		assertThat(posts).extracting(PostView::postId).containsExactly(newer, older);
	}
}
