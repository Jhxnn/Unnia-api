package com.Unnia.social.domain.repository;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Follow;

import java.util.List;
import java.util.Optional;

public interface FollowRepository {

	Follow save(Follow follow);

	Optional<Follow> findByFollowerAndFollowee(AccountId followerId, AccountId followeeId);

	void delete(Follow follow);

	List<Follow> findByFollower(AccountId followerId);

	long countByFollowee(AccountId followeeId);
}
