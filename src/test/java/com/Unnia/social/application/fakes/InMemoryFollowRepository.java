package com.Unnia.social.application.fakes;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Follow;
import com.Unnia.social.domain.model.FollowId;
import com.Unnia.social.domain.repository.FollowRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryFollowRepository implements FollowRepository {

	private final Map<FollowId, Follow> store = new LinkedHashMap<>();

	@Override
	public Follow save(Follow follow) {
		store.put(follow.id(), follow);
		return follow;
	}

	@Override
	public Optional<Follow> findByFollowerAndFollowee(AccountId followerId, AccountId followeeId) {
		return store.values().stream()
				.filter(f -> f.followerId().equals(followerId) && f.followeeId().equals(followeeId)).findFirst();
	}

	@Override
	public void delete(Follow follow) {
		store.remove(follow.id());
	}

	@Override
	public List<Follow> findByFollower(AccountId followerId) {
		return store.values().stream().filter(f -> f.followerId().equals(followerId)).toList();
	}

	@Override
	public long countByFollowee(AccountId followeeId) {
		return store.values().stream().filter(f -> f.followeeId().equals(followeeId)).count();
	}
}
