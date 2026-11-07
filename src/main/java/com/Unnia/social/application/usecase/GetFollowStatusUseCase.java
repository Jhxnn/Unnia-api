package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.repository.FollowRepository;

/** Se quem consulta já segue a conta, e quantas pessoas a seguem. */
public class GetFollowStatusUseCase {

	public record Status(boolean following, long followers) {
	}

	private final FollowRepository follows;

	public GetFollowStatusUseCase(FollowRepository follows) {
		this.follows = follows;
	}

	public Status execute(AccountId viewer, AccountId target) {
		return new Status(follows.findByFollowerAndFollowee(viewer, target).isPresent(),
				follows.countByFollowee(target));
	}
}
