package com.Unnia.moderation.domain.model;

import com.Unnia.moderation.domain.event.ReportDismissed;
import com.Unnia.moderation.domain.event.ReportFiled;
import com.Unnia.moderation.domain.event.ReportResolved;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final AccountId REPORTER = AccountId.generate();
	private static final AccountId MODERATOR = AccountId.generate();
	private static final ReportTarget POST = new ReportTarget(ReportTargetType.POST, UUID.randomUUID());

	private Report openReport() {
		return Report.file(REPORTER, POST, ReportReason.SPAM, null, NOW);
	}

	@Test
	void filesOpenReportAndRaisesEvent() {
		Report report = openReport();

		assertThat(report.status()).isEqualTo(ReportStatus.OPEN);
		assertThat(report.reporterId()).isEqualTo(REPORTER);
		assertThat(report.target()).isEqualTo(POST);
		assertThat(report.details()).isEmpty();
		assertThat(report.reviewedBy()).isEmpty();
		assertThat(report.pullDomainEvents()).containsExactly(new ReportFiled(report.id(), POST));
	}

	@Test
	void cannotReportOwnAccount() {
		ReportTarget self = new ReportTarget(ReportTargetType.ACCOUNT, REPORTER.value());

		assertThatThrownBy(() -> Report.file(REPORTER, self, ReportReason.ABUSE, null, NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void canReportOwnPostBecauseOnlyAccountsAreProtected() {
		ReportTarget sameIdAsPost = new ReportTarget(ReportTargetType.POST, REPORTER.value());

		assertThat(Report.file(REPORTER, sameIdAsPost, ReportReason.SPAM, null, NOW).status())
				.isEqualTo(ReportStatus.OPEN);
	}

	@Test
	void otherReasonRequiresDetails() {
		assertThatThrownBy(() -> Report.file(REPORTER, POST, ReportReason.OTHER, " ", NOW))
				.isInstanceOf(DomainException.class);
		assertThat(Report.file(REPORTER, POST, ReportReason.OTHER, "Conteúdo enganoso", NOW).details())
				.isEqualTo("Conteúdo enganoso");
	}

	@Test
	void limitsDetailsLength() {
		assertThatThrownBy(() -> Report.file(REPORTER, POST, ReportReason.ABUSE, "x".repeat(1001), NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void requiresReporterTargetAndReason() {
		assertThatThrownBy(() -> Report.file(null, POST, ReportReason.SPAM, null, NOW))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Report.file(REPORTER, null, ReportReason.SPAM, null, NOW))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> Report.file(REPORTER, POST, null, null, NOW)).isInstanceOf(DomainException.class);
	}

	@Test
	void moderatorResolvesReport() {
		Report report = openReport();
		report.pullDomainEvents();
		Instant later = NOW.plusSeconds(60);

		report.resolve(MODERATOR, later);

		assertThat(report.status()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(report.reviewedBy()).contains(MODERATOR);
		assertThat(report.reviewedAt()).contains(later);
		assertThat(report.pullDomainEvents()).containsExactly(new ReportResolved(report.id(), POST));
	}

	@Test
	void moderatorDismissesReport() {
		Report report = openReport();
		report.pullDomainEvents();

		report.dismiss(MODERATOR, NOW);

		assertThat(report.status()).isEqualTo(ReportStatus.DISMISSED);
		assertThat(report.pullDomainEvents()).containsExactly(new ReportDismissed(report.id()));
	}

	@Test
	void reviewIsFinal() {
		Report report = openReport();
		report.resolve(MODERATOR, NOW);

		assertThatThrownBy(() -> report.dismiss(MODERATOR, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> report.resolve(MODERATOR, NOW)).isInstanceOf(DomainException.class);
	}

	@Test
	void reporterCannotReviewOwnReport() {
		Report report = openReport();

		assertThatThrownBy(() -> report.resolve(REPORTER, NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> report.dismiss(null, NOW)).isInstanceOf(DomainException.class);
	}
}
