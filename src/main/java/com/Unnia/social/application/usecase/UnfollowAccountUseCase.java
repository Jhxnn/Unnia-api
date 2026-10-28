package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.repository.FollowRepository;

/** Idempotente: deixar de seguir quem não é seguido não é erro. */
public class UnfollowAccountUseCase {

	public record Command(AccountId followerId, AccountId followeeId) {
	}

	private final FollowRepository follows;

	public UnfollowAccountUseCase(FollowRepository follows) {
		this.follows = follows;
	}

	public void execute(Command command) {
		follows.findByFollowerAndFollowee(command.followerId(), command.followeeId()).ifPresent(follows::delete);
	}
}
