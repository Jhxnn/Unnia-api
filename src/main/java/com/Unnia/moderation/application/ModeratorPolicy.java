package com.Unnia.moderation.application;

import com.Unnia.moderation.application.port.ModeratorDirectory;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.domain.id.AccountId;

public class ModeratorPolicy {

	private final ModeratorDirectory moderators;

	public ModeratorPolicy(ModeratorDirectory moderators) {
		this.moderators = moderators;
	}

	public void requireModerator(AccountId accountId) {
		if (!moderators.isModerator(accountId)) {
			throw new ForbiddenException("Acesso restrito a moderadores");
		}
	}
}
