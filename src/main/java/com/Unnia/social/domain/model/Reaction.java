package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.event.ReactionAdded;

import java.time.Instant;

/**
 * Uma reação por autor e alvo. A unicidade é garantida pelo repositório
 * ({@code findByAuthorAndTarget}) antes de reagir novamente.
 */
public class Reaction extends AggregateRoot<ReactionId> {

	private final AccountId authorId;
	private final ReactionTarget target;
	private final Instant createdAt;
	private ReactionType type;

	private Reaction(ReactionId id, AccountId authorId, ReactionTarget target, ReactionType type,
			Instant createdAt) {
		super(id);
		this.authorId = Guard.notNull(authorId, "Autor é obrigatório");
		this.target = Guard.notNull(target, "Alvo é obrigatório");
		this.type = Guard.notNull(type, "Tipo da reação é obrigatório");
		this.createdAt = Guard.notNull(createdAt, "Data é obrigatória");
	}

	public static Reaction react(AccountId authorId, ReactionTarget target, ReactionType type, Instant now) {
		Reaction reaction = new Reaction(ReactionId.generate(), authorId, target, type, now);
		reaction.raise(new ReactionAdded(reaction.id(), authorId, target));
		return reaction;
	}

	public static Reaction restore(ReactionId id, AccountId authorId, ReactionTarget target, ReactionType type,
			Instant createdAt) {
		return new Reaction(id, authorId, target, type, createdAt);
	}

	public void changeType(ReactionType newType) {
		this.type = Guard.notNull(newType, "Tipo da reação é obrigatório");
	}

	public AccountId authorId() {
		return authorId;
	}

	public ReactionTarget target() {
		return target;
	}

	public ReactionType type() {
		return type;
	}

	public Instant createdAt() {
		return createdAt;
	}
}
