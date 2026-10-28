package com.Unnia.social.domain.repository;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Reaction;
import com.Unnia.social.domain.model.ReactionTarget;

import java.util.Optional;

public interface ReactionRepository {

	Reaction save(Reaction reaction);

	Optional<Reaction> findByAuthorAndTarget(AccountId authorId, ReactionTarget target);

	void delete(Reaction reaction);

	long countByTarget(ReactionTarget target);
}
