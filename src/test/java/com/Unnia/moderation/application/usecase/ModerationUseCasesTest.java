package com.Unnia.moderation.application.usecase;

import com.Unnia.moderation.application.ModeratorPolicy;
import com.Unnia.moderation.application.ReportView;
import com.Unnia.moderation.application.fakes.FakeModerationPorts;
import com.Unnia.moderation.application.fakes.FakeModerationPorts.TakeDown;
import com.Unnia.moderation.application.fakes.InMemoryReportRepository;
import com.Unnia.moderation.domain.event.ReportDismissed;
import com.Unnia.moderation.domain.event.ReportFiled;
import com.Unnia.moderation.domain.event.ReportResolved;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportReason;
import com.Unnia.moderation.domain.model.ReportStatus;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.model.ReportTargetType;
import com.Unnia.shared.application.ConflictException;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationUseCasesTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private FakeModerationPorts ports;
	private InMemoryReportRepository reports;
	private RecordingEventPublisher publisher;
	private ModeratorPolicy policy;
	private AccountId reporter;
	private AccountId moderator;
	private ReportTarget post;

	@BeforeEach
	void setUp() {
		ports = new FakeModerationPorts();
		reports = new InMemoryReportRepository();
		publisher = new RecordingEventPublisher();
		policy = new ModeratorPolicy(ports);
		reporter = AccountId.generate();
		moderator = ports.moderator();
		post = ports.content(ReportTargetType.POST);
	}

	private FileReportUseCase file() {
		return new FileReportUseCase(ports, reports, publisher, clock);
	}

	private ReportId fileSpam(AccountId by, ReportTarget target) {
		ReportId id = file().execute(new FileReportUseCase.Command(by, target, ReportReason.SPAM, null));
		clock.advance(Duration.ofMinutes(1));
		return id;
	}

	@Test
	void filesAReportAndPublishesEvent() {
		ReportId id = file().execute(new FileReportUseCase.Command(reporter, post, ReportReason.ABUSE, "ofensivo"));

		assertThat(reports.findById(id).orElseThrow().status()).isEqualTo(ReportStatus.OPEN);
		assertThat(publisher.published()).containsExactly(new ReportFiled(id, post));
	}

	@Test
	void cannotReportMissingContent() {
		ReportTarget unknown = new ReportTarget(ReportTargetType.COMMENT, UUID.randomUUID());

		assertThatThrownBy(() -> fileSpam(reporter, unknown)).isInstanceOf(NotFoundException.class);
		assertThat(publisher.published()).isEmpty();
	}

	@Test
	void cannotReportTheSameContentTwiceWhileOpenButCanAfterItIsReviewed() {
		ReportId first = fileSpam(reporter, post);

		assertThatThrownBy(() -> fileSpam(reporter, post)).isInstanceOf(ConflictException.class);
		assertThat(fileSpam(AccountId.generate(), post)).isNotNull();

		new DismissReportUseCase(policy, reports, publisher, clock)
				.execute(new DismissReportUseCase.Command(moderator, first));
		assertThat(fileSpam(reporter, post)).isNotEqualTo(first);
	}

	@Test
	void domainRulesStillApplyWhenFiling() {
		ReportTarget ownAccount = ports.content(ReportTargetType.ACCOUNT);
		ReportTarget self = new ReportTarget(ReportTargetType.ACCOUNT, reporter.value());

		assertThatThrownBy(() -> file().execute(
				new FileReportUseCase.Command(reporter, ownAccount, ReportReason.OTHER, " ")))
				.isInstanceOf(DomainException.class);
		assertThat(ports.exists(self)).isFalse();
	}

	@Test
	void moderatorsListOpenReportsOldestFirstWithPaging() {
		ReportId first = fileSpam(reporter, post);
		ReportId second = fileSpam(AccountId.generate(), post);
		ReportId third = fileSpam(AccountId.generate(), ports.content(ReportTargetType.COMMENT));
		new DismissReportUseCase(policy, reports, publisher, clock)
				.execute(new DismissReportUseCase.Command(moderator, second));
		ListOpenReportsUseCase useCase = new ListOpenReportsUseCase(policy, reports);

		List<ReportView> page0 = useCase.execute(moderator, 0, 1);
		List<ReportView> page1 = useCase.execute(moderator, 1, 1);

		assertThat(page0).extracting(ReportView::reportId).containsExactly(first);
		assertThat(page1).extracting(ReportView::reportId).containsExactly(third);
		assertThat(page0.get(0).status()).isEqualTo(ReportStatus.OPEN);
	}

	@Test
	void listingIsRestrictedToModeratorsAndValidatesPaging() {
		ListOpenReportsUseCase useCase = new ListOpenReportsUseCase(policy, reports);

		assertThatThrownBy(() -> useCase.execute(reporter, 0, 10)).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(moderator, -1, 10)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(moderator, 0, ListOpenReportsUseCase.MAX_PAGE_SIZE + 1))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void resolvingWithTakedownRemovesTheContent() {
		ReportId id = fileSpam(reporter, post);
		publisher.clear();

		new ResolveReportUseCase(policy, reports, ports, publisher, clock)
				.execute(new ResolveReportUseCase.Command(moderator, id, true));

		assertThat(reports.findById(id).orElseThrow().status()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(ports.takeDowns()).containsExactly(new TakeDown(moderator, post));
		assertThat(publisher.published()).containsExactly(new ReportResolved(id, post));
	}

	@Test
	void resolvingWithoutTakedownKeepsTheContent() {
		ReportId id = fileSpam(reporter, post);

		new ResolveReportUseCase(policy, reports, ports, publisher, clock)
				.execute(new ResolveReportUseCase.Command(moderator, id, false));

		assertThat(reports.findById(id).orElseThrow().status()).isEqualTo(ReportStatus.RESOLVED);
		assertThat(ports.takeDowns()).isEmpty();
	}

	@Test
	void dismissingNeverTouchesTheContent() {
		ReportId id = fileSpam(reporter, post);
		publisher.clear();

		new DismissReportUseCase(policy, reports, publisher, clock)
				.execute(new DismissReportUseCase.Command(moderator, id));

		assertThat(reports.findById(id).orElseThrow().status()).isEqualTo(ReportStatus.DISMISSED);
		assertThat(ports.takeDowns()).isEmpty();
		assertThat(publisher.published()).containsExactly(new ReportDismissed(id));
	}

	@Test
	void reviewIsRestrictedToModeratorsAndFinal() {
		ReportId id = fileSpam(reporter, post);
		ResolveReportUseCase resolve = new ResolveReportUseCase(policy, reports, ports, publisher, clock);
		DismissReportUseCase dismiss = new DismissReportUseCase(policy, reports, publisher, clock);

		assertThatThrownBy(() -> resolve.execute(new ResolveReportUseCase.Command(reporter, id, true)))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> dismiss.execute(new DismissReportUseCase.Command(reporter, id)))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> resolve.execute(new ResolveReportUseCase.Command(moderator, ReportId.generate(),
				true))).isInstanceOf(NotFoundException.class);

		resolve.execute(new ResolveReportUseCase.Command(moderator, id, false));
		assertThatThrownBy(() -> dismiss.execute(new DismissReportUseCase.Command(moderator, id)))
				.isInstanceOf(DomainException.class);
		assertThat(ports.takeDowns()).isEmpty();
	}

	@Test
	void aFailedReviewNeverTriggersATakedown() {
		ReportId id = fileSpam(moderator, post);

		assertThatThrownBy(() -> new ResolveReportUseCase(policy, reports, ports, publisher, clock)
				.execute(new ResolveReportUseCase.Command(moderator, id, true))).isInstanceOf(DomainException.class);

		assertThat(ports.takeDowns()).isEmpty();
		assertThat(reports.findById(id).orElseThrow().status()).isEqualTo(ReportStatus.OPEN);
	}
}
