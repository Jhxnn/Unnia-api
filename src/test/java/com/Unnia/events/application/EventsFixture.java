package com.Unnia.events.application;

import com.Unnia.events.application.fakes.FakeNgoDirectory;
import com.Unnia.events.application.fakes.FakeVolunteerDirectory;
import com.Unnia.events.application.fakes.InMemoryEventRepository;
import com.Unnia.events.application.usecase.CreateEventUseCase;
import com.Unnia.events.application.usecase.PublishEventUseCase;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

/** Cenário compartilhado pelos testes de casos de uso do contexto de eventos. */
public class EventsFixture {

	public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	public static final Instant START = NOW.plus(Duration.ofDays(7));
	public static final Instant END = START.plus(Duration.ofHours(4));

	public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	public final InMemoryEventRepository events = new InMemoryEventRepository();
	public final FakeNgoDirectory ngos = new FakeNgoDirectory();
	public final FakeVolunteerDirectory volunteers = new FakeVolunteerDirectory();
	public final RecordingEventPublisher publisher = new RecordingEventPublisher();
	public final EventOwnership ownership = new EventOwnership(ngos, events);

	public AccountId verifiedNgoAccount() {
		AccountId account = AccountId.generate();
		ngos.add(account, true);
		return account;
	}

	public AccountId unverifiedNgoAccount() {
		AccountId account = AccountId.generate();
		ngos.add(account, false);
		return account;
	}

	public AccountId volunteerAccount() {
		AccountId account = AccountId.generate();
		volunteers.add(account);
		return account;
	}

	public EventId draftEvent(AccountId ngoAccount, int capacity) {
		EventId id = new CreateEventUseCase(ngos, events, publisher, clock).execute(
				new CreateEventUseCase.Command(ngoAccount, "Mutirão de doação", "Arrecadação de alimentos",
						"Rua A, 10", "Recife", START, END, capacity));
		publisher.clear();
		return id;
	}

	public EventId publishedEvent(AccountId ngoAccount, int capacity) {
		EventId id = draftEvent(ngoAccount, capacity);
		new PublishEventUseCase(ownership, ngos, events, publisher, clock)
				.execute(new PublishEventUseCase.Command(ngoAccount, id));
		publisher.clear();
		return id;
	}
}
