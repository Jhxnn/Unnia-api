package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.ConversationView;
import com.Unnia.messaging.application.MessageView;
import com.Unnia.messaging.application.fakes.FakeMessagingDirectories;
import com.Unnia.messaging.application.fakes.InMemoryConversationRepository;
import com.Unnia.messaging.application.fakes.InMemoryMessageRepository;
import com.Unnia.messaging.application.port.ParticipantDirectory.Kind;
import com.Unnia.messaging.domain.event.ConversationStarted;
import com.Unnia.messaging.domain.event.MessageSent;
import com.Unnia.messaging.domain.model.ConversationId;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessagingUseCasesTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));
	private FakeMessagingDirectories directories;
	private InMemoryConversationRepository conversations;
	private InMemoryMessageRepository messages;
	private RecordingEventPublisher publisher;
	private AccountId ngo;
	private AccountId volunteer;

	@BeforeEach
	void setUp() {
		directories = new FakeMessagingDirectories();
		conversations = new InMemoryConversationRepository();
		messages = new InMemoryMessageRepository();
		publisher = new RecordingEventPublisher();
		ngo = directories.account(Kind.NGO);
		volunteer = directories.account(Kind.VOLUNTEER);
	}

	private SendMessageUseCase send() {
		return new SendMessageUseCase(directories, conversations, messages, publisher, clock);
	}

	private SendMessageUseCase.Result say(AccountId from, AccountId to, String text) {
		SendMessageUseCase.Result result = send().execute(new SendMessageUseCase.Command(from, to, text));
		clock.advance(Duration.ofMinutes(1));
		return result;
	}

	@Test
	void firstMessageOpensTheConversationAndPublishesEvents() {
		SendMessageUseCase.Result result = send().execute(new SendMessageUseCase.Command(volunteer, ngo, "Olá!"));

		assertThat(conversations.size()).isEqualTo(1);
		assertThat(messages.findById(result.messageId()).orElseThrow().recipientId()).isEqualTo(ngo);
		assertThat(publisher.published()).containsExactly(
				new ConversationStarted(result.conversationId(), volunteer, ngo),
				new MessageSent(result.messageId(), result.conversationId(), volunteer, ngo));
	}

	@Test
	void replyingReusesTheSameConversationInEitherDirection() {
		SendMessageUseCase.Result first = say(volunteer, ngo, "Olá!");
		SendMessageUseCase.Result second = say(ngo, volunteer, "Oi, tudo bem?");

		assertThat(second.conversationId()).isEqualTo(first.conversationId());
		assertThat(conversations.size()).isEqualTo(1);
		assertThat(messages.findByConversation(first.conversationId(), 0, 10)).hasSize(2);
	}

	@Test
	void onlyNgosAndVolunteersTalkToEachOther() {
		AccountId otherNgo = directories.account(Kind.NGO);
		AccountId otherVolunteer = directories.account(Kind.VOLUNTEER);
		AccountId admin = directories.account(Kind.OTHER);

		assertThatThrownBy(() -> say(ngo, otherNgo, "x")).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> say(volunteer, otherVolunteer, "x")).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> say(admin, ngo, "x")).isInstanceOf(ForbiddenException.class);
		assertThat(conversations.size()).isZero();
	}

	@Test
	void unknownOrInactiveParticipantsAreRejected() {
		AccountId suspended = directories.account(Kind.VOLUNTEER);
		directories.deactivate(suspended);

		assertThatThrownBy(() -> say(volunteer, AccountId.generate(), "x")).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> say(volunteer, suspended, "x")).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> say(suspended, ngo, "x")).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void rejectsBlankMessageWithoutPersistingAnything() {
		assertThatThrownBy(() -> say(volunteer, ngo, "  ")).isInstanceOf(DomainException.class);

		assertThat(publisher.published()).isEmpty();
		assertThat(messages.countUnreadFor(ngo)).isZero();
	}

	@Test
	void inboxListsConversationsNewestFirstWithUnreadCounts() {
		AccountId otherVolunteer = directories.account(Kind.VOLUNTEER);
		SendMessageUseCase.Result older = say(volunteer, ngo, "Primeira");
		say(volunteer, ngo, "Segunda");
		SendMessageUseCase.Result newer = say(otherVolunteer, ngo, "Oi");

		List<ConversationView> inbox = new ListConversationsUseCase(conversations, messages).execute(ngo);

		assertThat(inbox).extracting(ConversationView::conversationId).containsExactly(newer.conversationId(),
				older.conversationId());
		assertThat(inbox).extracting(ConversationView::unreadCount).containsExactly(1L, 2L);
		assertThat(inbox).extracting(ConversationView::otherAccountId).containsExactly(otherVolunteer, volunteer);
	}

	@Test
	void messagesAreReturnedNewestFirstAndPaginated() {
		SendMessageUseCase.Result first = say(volunteer, ngo, "um");
		say(ngo, volunteer, "dois");
		say(volunteer, ngo, "tres");
		GetMessagesUseCase useCase = new GetMessagesUseCase(conversations, messages);

		List<MessageView> page0 = useCase.execute(ngo, first.conversationId(), 0, 2);
		List<MessageView> page1 = useCase.execute(ngo, first.conversationId(), 1, 2);

		assertThat(page0).extracting(MessageView::text).containsExactly("tres", "dois");
		assertThat(page1).extracting(MessageView::text).containsExactly("um");
	}

	@Test
	void onlyParticipantsReadAConversation() {
		SendMessageUseCase.Result result = say(volunteer, ngo, "segredo");
		GetMessagesUseCase useCase = new GetMessagesUseCase(conversations, messages);

		assertThatThrownBy(() -> useCase.execute(directories.account(Kind.NGO), result.conversationId(), 0, 10))
				.isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(ngo, ConversationId.generate(), 0, 10))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> useCase.execute(ngo, result.conversationId(), -1, 10))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> useCase.execute(ngo, result.conversationId(), 0, 101))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void markingAsReadClearsOnlyTheReadersUnreadMessages() {
		SendMessageUseCase.Result result = say(volunteer, ngo, "para a ONG");
		say(ngo, volunteer, "para o voluntário");
		MarkConversationReadUseCase useCase = new MarkConversationReadUseCase(conversations, messages, clock);

		useCase.execute(new MarkConversationReadUseCase.Command(ngo, result.conversationId()));

		assertThat(messages.countUnreadFor(ngo)).isZero();
		assertThat(messages.countUnreadFor(volunteer)).isEqualTo(1);
	}

	@Test
	void markingAsReadIsRestrictedToParticipants() {
		SendMessageUseCase.Result result = say(volunteer, ngo, "oi");
		MarkConversationReadUseCase useCase = new MarkConversationReadUseCase(conversations, messages, clock);

		assertThatThrownBy(() -> useCase.execute(new MarkConversationReadUseCase.Command(
				directories.account(Kind.NGO), result.conversationId()))).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> useCase.execute(new MarkConversationReadUseCase.Command(ngo,
				ConversationId.generate()))).isInstanceOf(NotFoundException.class);
		assertThat(messages.countUnreadFor(ngo)).isEqualTo(1);
	}
}
