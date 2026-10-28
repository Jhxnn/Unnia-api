package com.Unnia.messaging.application.handler;

import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.DomainEventHandler;
import com.Unnia.social.domain.event.AccountFollowed;

public class AccountFollowedHandler implements DomainEventHandler<AccountFollowed> {

	private final Notifier notifier;

	public AccountFollowedHandler(Notifier notifier) {
		this.notifier = notifier;
	}

	@Override
	public Class<AccountFollowed> eventType() {
		return AccountFollowed.class;
	}

	@Override
	public void handle(AccountFollowed event) {
		notifier.notify(event.followeeId(), NotificationType.NEW_FOLLOWER, "Você tem um novo seguidor",
				event.followerId().value());
	}
}
