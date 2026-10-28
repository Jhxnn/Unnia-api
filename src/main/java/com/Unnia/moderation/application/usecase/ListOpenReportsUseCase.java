package com.Unnia.moderation.application.usecase;

import com.Unnia.moderation.application.ModeratorPolicy;
import com.Unnia.moderation.application.ReportView;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.repository.ReportRepository;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;

import java.util.List;

public class ListOpenReportsUseCase {

	public static final int MAX_PAGE_SIZE = 50;

	private final ModeratorPolicy policy;
	private final ReportRepository reports;

	public ListOpenReportsUseCase(ModeratorPolicy policy, ReportRepository reports) {
		this.policy = policy;
		this.reports = reports;
	}

	public List<ReportView> execute(AccountId moderatorId, int page, int size) {
		policy.requireModerator(moderatorId);
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);
		return reports.findByStatus(ReportStatus.OPEN, page, size).stream().map(ReportView::from).toList();
	}
}
