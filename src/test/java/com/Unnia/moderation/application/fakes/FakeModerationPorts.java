package com.Unnia.moderation.application.fakes;

import com.Unnia.moderation.application.port.ContentTakedown;
import com.Unnia.moderation.application.port.ModeratorDirectory;
import com.Unnia.moderation.application.port.ReportableContent;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.model.ReportTargetType;
import com.Unnia.shared.domain.id.AccountId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Implementa as três portas do contexto de moderação com dados em memória. */
public class FakeModerationPorts implements ModeratorDirectory, ReportableContent, ContentTakedown {

	public record TakeDown(AccountId moderatorId, ReportTarget target) {
	}

	private final Set<AccountId> moderators = new HashSet<>();
	private final Set<ReportTarget> existing = new HashSet<>();
	private final List<TakeDown> takeDowns = new ArrayList<>();

	public AccountId moderator() {
		AccountId id = AccountId.generate();
		moderators.add(id);
		return id;
	}

	public ReportTarget content(ReportTargetType type) {
		ReportTarget target = new ReportTarget(type, UUID.randomUUID());
		existing.add(target);
		return target;
	}

	public List<TakeDown> takeDowns() {
		return List.copyOf(takeDowns);
	}

	@Override
	public boolean isModerator(AccountId accountId) {
		return moderators.contains(accountId);
	}

	@Override
	public boolean exists(ReportTarget target) {
		return existing.contains(target);
	}

	@Override
	public void takeDown(AccountId moderatorId, ReportTarget target) {
		takeDowns.add(new TakeDown(moderatorId, target));
	}
}
