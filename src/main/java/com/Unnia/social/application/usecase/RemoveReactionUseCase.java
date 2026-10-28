package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.ReactionTarget;
import com.Unnia.social.domain.repository.ReactionRepository;

/** Idempotente: remover uma reação que não existe não é erro. */
public class RemoveReactionUseCase {

	public record Command(AccountId accountId, ReactionTarget target) {
	}

	private final ReactionRepository reactions;

	public RemoveReactionUseCase(ReactionRepository reactions) {
		this.reactions = reactions;
	}

	public void execute(Command command) {
		reactions.findByAuthorAndTarget(command.accountId(), command.target()).ifPresent(reactions::delete);
	}
}
