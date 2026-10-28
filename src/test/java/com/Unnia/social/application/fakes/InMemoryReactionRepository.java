package com.Unnia.social.application.fakes;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.repository.ReactionRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryReactionRepository implements ReactionRepository {

	private final Map<ReactionId, Reaction> store = new LinkedHashMap<>();

	@Override
	public Reaction save(Reaction reaction) {
		store.put(reaction.id(), reaction);
		return reaction;
	}

	@Override
	public Optional<Reaction> findByAuthorAndTarget(AccountId authorId, ReactionTarget target) {
		return store.values().stream()
				.filter(r -> r.authorId().equals(authorId) && r.target().equals(target)).findFirst();
	}

	@Override
	public void delete(Reaction reaction) {
		store.remove(reaction.id());
	}

	@Override
	public long countByTarget(ReactionTarget target) {
		return store.values().stream().filter(r -> r.target().equals(target)).count();
	}
}
