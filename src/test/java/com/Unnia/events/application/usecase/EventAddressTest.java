package com.Unnia.events.application.usecase;

import com.Unnia.events.application.EventView;
import com.Unnia.events.application.EventsFixture;
import com.Unnia.events.domain.model.Event;
import com.Unnia.events.infrastructure.persistence.EventAddressRoundTrip;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.Unnia.events.application.EventsFixture.START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Estado e CEP do evento: opcionais, mas válidos quando informados. */
class EventAddressTest {

	private EventsFixture fx;
	private AccountId ngo;

	@BeforeEach
	void setUp() {
		fx = new EventsFixture();
		ngo = fx.verifiedNgoAccount();
	}

	private EventId create(String state, String cep) {
		return new CreateEventUseCase(fx.ngos, fx.events, fx.publisher, fx.clock).execute(
				new CreateEventUseCase.Command(ngo, "Mutirão", "Desc", "Rua do Futuro, 120 - Aflitos", "Recife", state,
						cep, START, START.plus(Duration.ofHours(2)), 10));
	}

	@Test
	void anEventCanCarryItsStateAndCep() {
		EventId id = create("pe", "50030-230");

		Event event = fx.events.findById(id).orElseThrow();
		assertThat(event.location().state().value()).isEqualTo("PE");
		assertThat(event.location().cep().value()).isEqualTo("50030230");
		EventView view = EventView.from(event);
		assertThat(view.state()).isEqualTo("PE");
		assertThat(view.cep()).isEqualTo("50030-230");
	}

	@Test
	void stateAndCepAreOptional() {
		EventView view = EventView.from(fx.events.findById(create(null, "")).orElseThrow());

		assertThat(view.state()).isNull();
		assertThat(view.cep()).isNull();
		assertThat(view.city()).isEqualTo("Recife");
	}

	@Test
	void anInvalidCepOrStateIsRejectedAndNothingIsSaved() {
		assertThatThrownBy(() -> create("PE", "123")).isInstanceOf(DomainException.class).hasMessage("CEP deve ter 8 dígitos");
		assertThatThrownBy(() -> create("XX", "50030-230")).isInstanceOf(DomainException.class);
		assertThat(fx.publisher.published()).isEmpty();
	}

	@Test
	void editingTheDetailsCanChangeOrClearTheStateAndCep() {
		EventId id = create("PE", "50030-230");
		UpdateEventDetailsUseCase update = new UpdateEventDetailsUseCase(fx.ownership, fx.events);

		update.execute(new UpdateEventDetailsUseCase.Command(ngo, id, "Mutirão", "Desc", "Av. Paulista, 1000",
				"São Paulo", "sp", "01310-100"));
		Event moved = fx.events.findById(id).orElseThrow();
		assertThat(moved.location().state().value()).isEqualTo("SP");
		assertThat(moved.location().cep().formatted()).isEqualTo("01310-100");

		update.execute(new UpdateEventDetailsUseCase.Command(ngo, id, "Mutirão", "Desc", "Rua sem CEP", "Recife", "", ""));
		Event cleared = fx.events.findById(id).orElseThrow();
		assertThat(cleared.location().cep()).isNull();
		assertThat(cleared.location().state()).isNull();
	}

	@Test
	void editingWithoutStateAndCepKeepsTheOnesTheEventAlreadyHas() {
		EventId id = create("PE", "50030-230");

		new UpdateEventDetailsUseCase(fx.ownership, fx.events).execute(
				new UpdateEventDetailsUseCase.Command(ngo, id, "Novo título", "Desc", "Rua B, 2", "Olinda"));

		Event kept = fx.events.findById(id).orElseThrow();
		assertThat(kept.title().value()).isEqualTo("Novo título");
		assertThat(kept.location().state().value()).isEqualTo("PE");
		assertThat(kept.location().cep().value()).isEqualTo("50030230");
	}

	@Test
	void theAddressSurvivesThePersistenceRoundTrip() {
		Event event = fx.events.findById(create("PE", "50030-230")).orElseThrow();

		Event restored = EventAddressRoundTrip.through(event);

		assertThat(restored.location()).isEqualTo(event.location());
		assertThat(restored.location().state().value()).isEqualTo("PE");
	}
}
