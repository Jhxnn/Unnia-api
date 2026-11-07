package com.Unnia.moderation.application.usecase;

import com.Unnia.moderation.application.ModeratorPolicy;
import com.Unnia.moderation.application.ReportView;
import com.Unnia.moderation.application.fakes.FakeModerationPorts;
import com.Unnia.moderation.application.fakes.InMemoryReportRepository;
import com.Unnia.moderation.domain.model.ReportReason;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.moderation.domain.model.ReportTargetType;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A fila de denúncias mostra ao moderador o que foi denunciado, não só um identificador. */
class ReportTargetSummaryTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private FakeModerationPorts ports;
	private InMemoryReportRepository reports;
	private FileReportUseCase file;
	private ListOpenReportsUseCase list;
	private AccountId moderator;

	@BeforeEach
	void setUp() {
		ports = new FakeModerationPorts();
		reports = new InMemoryReportRepository();
		file = new FileReportUseCase(ports, reports, new RecordingEventPublisher(), clock);
		list = new ListOpenReportsUseCase(new ModeratorPolicy(ports), reports, ports);
		moderator = ports.moderator();
	}

	private void report(ReportTarget target) {
		file.execute(new FileReportUseCase.Command(AccountId.generate(), target, ReportReason.SPAM, null));
		clock.advance(Duration.ofMinutes(1));
	}

	@Test
	void eachReportCarriesAReadableSummaryOfItsTarget() {
		report(ports.content(ReportTargetType.POST, "Compre seguidores aqui"));
		report(ports.content(ReportTargetType.COMMENT, "Você é horrível"));
		report(ports.content(ReportTargetType.ACCOUNT, "spammer@exemplo.com"));

		List<ReportView> open = list.execute(moderator, 0, 10);

		assertThat(open).extracting(ReportView::targetSummary)
				.containsExactly("Compre seguidores aqui", "Você é horrível", "spammer@exemplo.com");
		assertThat(open).extracting(view -> view.target().type())
				.containsExactly(ReportTargetType.POST, ReportTargetType.COMMENT, ReportTargetType.ACCOUNT);
	}

	@Test
	void whenTheContentIsGoneTheSummaryIsNullButTheReportStillShows() {
		report(ports.content(ReportTargetType.POST));

		assertThat(list.execute(moderator, 0, 10)).singleElement().satisfies(view -> {
			assertThat(view.targetSummary()).isNull();
			assertThat(view.reason()).isEqualTo(ReportReason.SPAM);
		});
	}
}
