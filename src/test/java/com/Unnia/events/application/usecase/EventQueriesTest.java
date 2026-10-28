package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventView;
import com.Unnia.events.application.EventsFixture;
import com.Unnia.events.domain.model.EventStatus;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.Unnia.events.application.EventsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventQueriesTest {

	private EventsFixture fx;
	private AccountId ngo;

	@BeforeEach
	void setUp() {
		fx = new EventsFixture();
		ngo = fx.verifiedNgoAccount();
	}

	private GetEventUseCase getEvent() {
		return new GetEventUseCase(fx.ngos, fx.events);
	}

	@Test
	void publishedEventIsVisibleToEveryoneIncludingVisitors() {
		EventId id = fx.publishedEvent(ngo, 10);

		EventView view = getEvent().execute(id, null);

		assertThat(view.title()).isEqualTo("Mutirão de doação");
		assertThat(view.status()).isEqualTo(EventStatus.PUBLISHED);
		assertThat(view.capacity()).isEqualTo(10);
		assertThat(view.availableSpots()).isEqualTo(10);
		assertThat(view.city()).isEqualTo("Recife");
	}

	@Test
	void draftIsVisibleOnlyToItsOwner() {
		EventId id = fx.draftEvent(ngo, 10);

		assertThat(getEvent().execute(id, ngo).status()).isEqualTo(EventStatus.DRAFT);
		assertThatThrownBy(() -> getEvent().execute(id, null)).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> getEvent().execute(id, fx.volunteerAccount())).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> getEvent().execute(id, fx.verifiedNgoAccount()))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void unknownEventIsNotFound() {
		assertThatThrownBy(() -> getEvent().execute(EventId.generate(), ngo)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void listsOnlyPublishedUpcomingEventsOrderedByStartWithPaging() {
		fx.draftEvent(ngo, 5);
		EventId first = fx.publishedEvent(ngo, 5);
		EventId second = laterPublishedEvent(Duration.ofDays(1));
		EventId third = laterPublishedEvent(Duration.ofDays(2));
		ListUpcomingEventsUseCase useCase = new ListUpcomingEventsUseCase(fx.events, fx.clock);

		List<EventView> firstPage = useCase.execute(0, 2);
		List<EventView> secondPage = useCase.execute(1, 2);

		assertThat(firstPage).extracting(EventView::eventId).containsExactly(first, second);
		assertThat(secondPage).extracting(EventView::eventId).containsExactly(third);
	}

	private EventId laterPublishedEvent(Duration offset) {
		EventId id = new CreateEventUseCase(fx.ngos, fx.events, fx.publisher, fx.clock).execute(
				new CreateEventUseCase.Command(ngo, "Outro", "d", "Rua", "Recife", START.plus(offset),
						START.plus(offset).plus(Duration.ofHours(2)), 5));
		new PublishEventUseCase(fx.ownership, fx.ngos, fx.events, fx.publisher, fx.clock)
				.execute(new PublishEventUseCase.Command(ngo, id));
		return id;
	}

	@Test
	void listingValidatesPaging() {
		ListUpcomingEventsUseCase useCase = new ListUpcomingEventsUseCase(fx.events, fx.clock);

		assertThatThrownBy(() -> useCase.execute(-1, 10)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(0, 0)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(0, ListUpcomingEventsUseCase.MAX_PAGE_SIZE + 1))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void ngoSeesAllItsOwnEventsIncludingDrafts() {
		fx.draftEvent(ngo, 5);
		fx.publishedEvent(ngo, 5);
		fx.publishedEvent(fx.verifiedNgoAccount(), 5);

		List<EventView> mine = new ListNgoEventsUseCase(fx.ngos, fx.events).execute(ngo);

		assertThat(mine).hasSize(2);
		assertThat(mine).extracting(EventView::status)
				.containsExactlyInAnyOrder(EventStatus.DRAFT, EventStatus.PUBLISHED);
	}

	@Test
	void volunteersCannotListNgoEvents() {
		assertThatThrownBy(() -> new ListNgoEventsUseCase(fx.ngos, fx.events).execute(fx.volunteerAccount()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void volunteerSeesOnlyEventsWithActiveRegistration() {
		EventId joined = fx.publishedEvent(ngo, 5);
		EventId left = fx.publishedEvent(ngo, 5);
		fx.publishedEvent(ngo, 5);
		AccountId volunteer = fx.volunteerAccount();
		RegisterForEventUseCase register = new RegisterForEventUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock);
		register.execute(new RegisterForEventUseCase.Command(volunteer, joined));
		register.execute(new RegisterForEventUseCase.Command(volunteer, left));
		new CancelRegistrationUseCase(fx.volunteers, fx.events, fx.publisher, fx.clock)
				.execute(new CancelRegistrationUseCase.Command(volunteer, left));

		List<EventView> mine = new ListMyRegistrationsUseCase(fx.volunteers, fx.events).execute(volunteer);

		assertThat(mine).extracting(EventView::eventId).containsExactly(joined);
	}

	@Test
	void ngosHaveNoRegistrations() {
		assertThatThrownBy(() -> new ListMyRegistrationsUseCase(fx.volunteers, fx.events).execute(ngo))
				.isInstanceOf(ForbiddenException.class);
	}
}
