package com.Unnia.events.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.events.application.EventView;
import com.Unnia.events.application.usecase.CancelEventUseCase;
import com.Unnia.events.application.usecase.CancelRegistrationUseCase;
import com.Unnia.events.application.usecase.ChangeEventCapacityUseCase;
import com.Unnia.events.application.usecase.CreateEventUseCase;
import com.Unnia.events.application.usecase.FinishEventUseCase;
import com.Unnia.events.application.usecase.GetEventUseCase;
import com.Unnia.events.application.usecase.ListEventRegistrationsUseCase;
import com.Unnia.events.application.usecase.ListMyRegistrationsUseCase;
import com.Unnia.events.application.usecase.ListNgoEventsUseCase;
import com.Unnia.events.application.usecase.ListUpcomingEventsUseCase;
import com.Unnia.events.application.usecase.PublishEventUseCase;
import com.Unnia.events.application.usecase.RecordAttendanceUseCase;
import com.Unnia.events.application.usecase.RegisterForEventUseCase;
import com.Unnia.events.application.usecase.RescheduleEventUseCase;
import com.Unnia.events.application.usecase.UpdateEventDetailsUseCase;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.VolunteerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Eventos")
@RestController
@RequestMapping("/api/events")
class EventController {

	record CreateEventRequest(@NotBlank String title, String description, @NotBlank String address,
			@NotBlank String city, String state, String cep, @NotNull Instant start, @NotNull Instant end, @Min(1) int capacity) {
	}

	record UpdateEventRequest(@NotBlank String title, String description, @NotBlank String address,
			@NotBlank String city, String state, String cep) {
	}

	record RescheduleRequest(@NotNull Instant start, @NotNull Instant end) {
	}

	record CapacityRequest(@Min(1) int capacity) {
	}

	record Created(EventId eventId) {
	}

	private final CreateEventUseCase create;
	private final PublishEventUseCase publish;
	private final UpdateEventDetailsUseCase update;
	private final RescheduleEventUseCase reschedule;
	private final ChangeEventCapacityUseCase changeCapacity;
	private final CancelEventUseCase cancel;
	private final FinishEventUseCase finish;
	private final RecordAttendanceUseCase recordAttendance;
	private final RegisterForEventUseCase register;
	private final CancelRegistrationUseCase cancelRegistration;
	private final GetEventUseCase get;
	private final ListUpcomingEventsUseCase listUpcoming;
	private final ListNgoEventsUseCase listMine;
	private final ListMyRegistrationsUseCase listRegistered;
	private final ListEventRegistrationsUseCase listRegistrants;

	EventController(CreateEventUseCase create, PublishEventUseCase publish, UpdateEventDetailsUseCase update,
			RescheduleEventUseCase reschedule, ChangeEventCapacityUseCase changeCapacity, CancelEventUseCase cancel,
			FinishEventUseCase finish, RecordAttendanceUseCase recordAttendance, RegisterForEventUseCase register,
			CancelRegistrationUseCase cancelRegistration, GetEventUseCase get, ListUpcomingEventsUseCase listUpcoming,
			ListNgoEventsUseCase listMine, ListMyRegistrationsUseCase listRegistered,
			ListEventRegistrationsUseCase listRegistrants) {
		this.create = create;
		this.publish = publish;
		this.update = update;
		this.reschedule = reschedule;
		this.changeCapacity = changeCapacity;
		this.cancel = cancel;
		this.finish = finish;
		this.recordAttendance = recordAttendance;
		this.register = register;
		this.cancelRegistration = cancelRegistration;
		this.get = get;
		this.listUpcoming = listUpcoming;
		this.listMine = listMine;
		this.listRegistered = listRegistered;
		this.listRegistrants = listRegistrants;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Created create(@AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody CreateEventRequest request) {
		return new Created(create.execute(new CreateEventUseCase.Command(user.accountId(), request.title(),
				request.description(), request.address(), request.city(), request.state(), request.cep(), request.start(),
				request.end(),
				request.capacity())));
	}

	@GetMapping
	List<EventView> upcoming(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return listUpcoming.execute(page, size);
	}

	@GetMapping("/mine")
	List<EventView> mine(@AuthenticationPrincipal AuthenticatedUser user) {
		return listMine.execute(user.accountId());
	}

	@GetMapping("/registered")
	List<EventView> registered(@AuthenticationPrincipal AuthenticatedUser user) {
		return listRegistered.execute(user.accountId());
	}

	/** Pública; um usuário autenticado dono do evento também enxerga rascunhos. */
	@GetMapping("/{id}")
	EventView byId(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		AccountId requester = user == null ? null : user.accountId();
		return get.execute(new EventId(id), requester);
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody UpdateEventRequest request) {
		update.execute(new UpdateEventDetailsUseCase.Command(user.accountId(), new EventId(id), request.title(),
				request.description(), request.address(), request.city(), request.state(), request.cep()));
	}

	@PutMapping("/{id}/schedule")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reschedule(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody RescheduleRequest request) {
		reschedule.execute(new RescheduleEventUseCase.Command(user.accountId(), new EventId(id), request.start(),
				request.end()));
	}

	@PutMapping("/{id}/capacity")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void capacity(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody CapacityRequest request) {
		changeCapacity.execute(new ChangeEventCapacityUseCase.Command(user.accountId(), new EventId(id),
				request.capacity()));
	}

	@PostMapping("/{id}/publish")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void publish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		publish.execute(new PublishEventUseCase.Command(user.accountId(), new EventId(id)));
	}

	@PostMapping("/{id}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		cancel.execute(new CancelEventUseCase.Command(user.accountId(), new EventId(id)));
	}

	@PostMapping("/{id}/finish")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void finish(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		finish.execute(new FinishEventUseCase.Command(user.accountId(), new EventId(id)));
	}

	@PostMapping("/{id}/attendance/{volunteerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void attendance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@PathVariable UUID volunteerId) {
		recordAttendance.execute(new RecordAttendanceUseCase.Command(user.accountId(), new EventId(id),
				new VolunteerId(volunteerId)));
	}

	/** Quem está inscrito: só a ONG dona do evento enxerga a lista. */
	@GetMapping("/{id}/registrations")
	List<ListEventRegistrationsUseCase.Registrant> registrants(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id) {
		return listRegistrants.execute(user.accountId(), new EventId(id));
	}

	@PostMapping("/{id}/registrations")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void register(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		register.execute(new RegisterForEventUseCase.Command(user.accountId(), new EventId(id)));
	}

	@DeleteMapping("/{id}/registrations")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void cancelRegistration(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		cancelRegistration.execute(new CancelRegistrationUseCase.Command(user.accountId(), new EventId(id)));
	}
}
