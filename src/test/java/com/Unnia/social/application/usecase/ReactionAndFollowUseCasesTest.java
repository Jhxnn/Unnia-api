package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.domain.event.AccountFollowed;
import com.Unnia.social.domain.event.ReactionAdded;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.model.ReactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactionAndFollowUseCasesTest {

	private SocialFixture fx;
	private AccountId author;
	private AccountId fan;
	private PostId postId;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		author = fx.account();
		fan = fx.account();
		postId = fx.post(author, "Post");
	}

	private ReactUseCase react() {
		return new ReactUseCase(fx.posts, fx.comments, fx.reactions, fx.publisher, fx.clock);
	}

	private FollowAccountUseCase follow() {
		return new FollowAccountUseCase(fx.directories, fx.follows, fx.publisher, fx.clock);
	}

	@Test
	void reactsToPostAndPublishesEventOnce() {
		ReactionTarget target = ReactionTarget.post(postId);

		react().execute(new ReactUseCase.Command(fan, target, ReactionType.LIKE));

		Reaction reaction = fx.reactions.findByAuthorAndTarget(fan, target).orElseThrow();
		assertThat(reaction.type()).isEqualTo(ReactionType.LIKE);
		assertThat(fx.publisher.published()).containsExactly(new ReactionAdded(reaction.id(), fan, target));
	}

	@Test
	void reactingAgainChangesTheTypeWithoutDuplicatingOrRepublishing() {
		ReactionTarget target = ReactionTarget.post(postId);
		react().execute(new ReactUseCase.Command(fan, target, ReactionType.LIKE));
		fx.publisher.clear();

		react().execute(new ReactUseCase.Command(fan, target, ReactionType.SUPPORT));

		assertThat(fx.reactions.countByTarget(target)).isEqualTo(1);
		assertThat(fx.reactions.findByAuthorAndTarget(fan, target).orElseThrow().type())
				.isEqualTo(ReactionType.SUPPORT);
		assertThat(fx.publisher.published()).isEmpty();
	}

	@Test
	void reactsToComments() {
		CommentId commentId = fx.comment(fan, postId, "Oi");
		ReactionTarget target = ReactionTarget.comment(commentId);

		react().execute(new ReactUseCase.Command(author, target, ReactionType.LOVE));

		assertThat(fx.reactions.countByTarget(target)).isEqualTo(1);
	}

	@Test
	void cannotReactToMissingOrDeletedTargets() {
		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(author, postId));

		assertThatThrownBy(() -> react().execute(
				new ReactUseCase.Command(fan, ReactionTarget.post(postId), ReactionType.LIKE)))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> react().execute(
				new ReactUseCase.Command(fan, ReactionTarget.comment(CommentId.generate()), ReactionType.LIKE)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void removesReactionAndIsIdempotent() {
		ReactionTarget target = ReactionTarget.post(postId);
		react().execute(new ReactUseCase.Command(fan, target, ReactionType.LIKE));
		RemoveReactionUseCase remove = new RemoveReactionUseCase(fx.reactions);

		remove.execute(new RemoveReactionUseCase.Command(fan, target));
		remove.execute(new RemoveReactionUseCase.Command(fan, target));

		assertThat(fx.reactions.countByTarget(target)).isZero();
	}

	@Test
	void followsAnotherAccount() {
		follow().execute(new FollowAccountUseCase.Command(fan, author));

		assertThat(fx.follows.findByFollowerAndFollowee(fan, author)).isPresent();
		assertThat(fx.follows.countByFollowee(author)).isEqualTo(1);
		assertThat(fx.publisher.published()).containsExactly(new AccountFollowed(fan, author));
	}

	@Test
	void followingTwiceIsIdempotent() {
		follow().execute(new FollowAccountUseCase.Command(fan, author));
		fx.publisher.clear();

		follow().execute(new FollowAccountUseCase.Command(fan, author));

		assertThat(fx.follows.countByFollowee(author)).isEqualTo(1);
		assertThat(fx.publisher.published()).isEmpty();
	}

	@Test
	void cannotFollowSelfOrUnknownAccount() {
		assertThatThrownBy(() -> follow().execute(new FollowAccountUseCase.Command(fan, fan)))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> follow().execute(new FollowAccountUseCase.Command(fan, AccountId.generate())))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void unfollowsAndIsIdempotent() {
		follow().execute(new FollowAccountUseCase.Command(fan, author));
		UnfollowAccountUseCase unfollow = new UnfollowAccountUseCase(fx.follows);

		unfollow.execute(new UnfollowAccountUseCase.Command(fan, author));
		unfollow.execute(new UnfollowAccountUseCase.Command(fan, author));

		assertThat(fx.follows.countByFollowee(author)).isZero();
	}
}
