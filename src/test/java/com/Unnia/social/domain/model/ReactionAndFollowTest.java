package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.event.AccountFollowed;
import com.Unnia.social.domain.event.ReactionAdded;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReactionAndFollowTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	@Test
	void reactsToPostAndRaisesEvent() {
		AccountId author = AccountId.generate();
		ReactionTarget target = ReactionTarget.post(PostId.generate());

		Reaction reaction = Reaction.react(author, target, ReactionType.LIKE, NOW);

		assertThat(reaction.type()).isEqualTo(ReactionType.LIKE);
		assertThat(reaction.target().type()).isEqualTo(TargetType.POST);
		assertThat(reaction.pullDomainEvents()).containsExactly(new ReactionAdded(reaction.id(), author, target));
	}

	@Test
	void targetsComment() {
		CommentId commentId = CommentId.generate();

		assertThat(ReactionTarget.comment(commentId))
				.isEqualTo(new ReactionTarget(TargetType.COMMENT, commentId.value()));
	}

	@Test
	void changesReactionType() {
		Reaction reaction = Reaction.react(AccountId.generate(), ReactionTarget.post(PostId.generate()),
				ReactionType.LIKE, NOW);

		reaction.changeType(ReactionType.SUPPORT);

		assertThat(reaction.type()).isEqualTo(ReactionType.SUPPORT);
		assertThatThrownBy(() -> reaction.changeType(null)).isInstanceOf(DomainException.class);
	}

	@Test
	void reactionRequiresAllData() {
		ReactionTarget target = ReactionTarget.post(PostId.generate());

		assertThatThrownBy(() -> Reaction.react(null, target, ReactionType.LIKE, NOW))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Reaction.react(AccountId.generate(), null, ReactionType.LIKE, NOW))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Reaction.react(AccountId.generate(), target, null, NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void followsAnotherAccountAndRaisesEvent() {
		AccountId follower = AccountId.generate();
		AccountId followee = AccountId.generate();

		Follow follow = Follow.create(follower, followee, NOW);

		assertThat(follow.followerId()).isEqualTo(follower);
		assertThat(follow.followeeId()).isEqualTo(followee);
		assertThat(follow.since()).isEqualTo(NOW);
		assertThat(follow.pullDomainEvents()).containsExactly(new AccountFollowed(follower, followee));
	}

	@Test
	void cannotFollowSelf() {
		AccountId account = AccountId.generate();

		assertThatThrownBy(() -> Follow.create(account, account, NOW)).isInstanceOf(DomainException.class);
	}
}
