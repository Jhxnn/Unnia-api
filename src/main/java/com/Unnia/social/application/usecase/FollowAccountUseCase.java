package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.port.AccountDirectory;
import com.Unnia.social.domain.model.Follow;
import com.Unnia.social.domain.repository.FollowRepository;

import java.time.Clock;

/** Idempotente: seguir quem já é seguido não gera erro nem evento. */
public class FollowAccountUseCase {

	public record Command(AccountId followerId, AccountId followeeId) {
	}

	private final AccountDirectory accounts;
	private final FollowRepository follows;
	private final DomainEventPublisher publisher;
	private final Clock clock;

	public FollowAccountUseCase(AccountDirectory accounts, FollowRepository follows, DomainEventPublisher publisher,
			Clock clock) {
		this.accounts = accounts;
		this.follows = follows;
		this.publisher = publisher;
		this.clock = clock;
	}

	public void execute(Command command) {
		if (!accounts.isActive(command.followeeId())) {
			throw new NotFoundException("Conta não encontrada");
		}
		if (follows.findByFollowerAndFollowee(command.followerId(), command.followeeId()).isPresent()) {
			return;
		}

		Follow follow = Follow.create(command.followerId(), command.followeeId(), clock.instant());

		follows.save(follow);
		publisher.publishFrom(follow);
	}
}
