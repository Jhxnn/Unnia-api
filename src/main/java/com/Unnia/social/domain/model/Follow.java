package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.event.AccountFollowed;

import java.time.Instant;

public class Follow extends AggregateRoot<FollowId> {

	private final AccountId followerId;
	private final AccountId followeeId;
	private final Instant since;

	private Follow(FollowId id, AccountId followerId, AccountId followeeId, Instant since) {
		super(id);
		this.followerId = Guard.notNull(followerId, "Seguidor é obrigatório");
		this.followeeId = Guard.notNull(followeeId, "Seguido é obrigatório");
		Guard.check(!followerId.equals(followeeId), "Uma conta não pode seguir a si mesma");
		this.since = Guard.notNull(since, "Data é obrigatória");
	}

	public static Follow create(AccountId followerId, AccountId followeeId, Instant now) {
		Follow follow = new Follow(FollowId.generate(), followerId, followeeId, now);
		follow.raise(new AccountFollowed(followerId, followeeId));
		return follow;
	}

	public static Follow restore(FollowId id, AccountId followerId, AccountId followeeId, Instant since) {
		return new Follow(id, followerId, followeeId, since);
	}

	public AccountId followerId() {
		return followerId;
	}

	public AccountId followeeId() {
		return followeeId;
	}

	public Instant since() {
		return since;
	}
}
