package com.Unnia.messaging.application.usecase;

import com.Unnia.messaging.application.ConversationView;
import com.Unnia.messaging.application.fakes.FakeMessagingDirectories;
import com.Unnia.messaging.application.fakes.InMemoryConversationRepository;
import com.Unnia.messaging.application.fakes.InMemoryMessageRepository;
import com.Unnia.messaging.application.port.ParticipantDirectory.Kind;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.application.RecordingEventPublisher;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A caixa de entrada mostra com quem é cada conversa, não só um identificador. */
class ConversationNamesTest {

	private final FakeMessagingDirectories directories = new FakeMessagingDirectories();
	private final InMemoryConversationRepository conversations = new InMemoryConversationRepository();
	private final InMemoryMessageRepository messages = new InMemoryMessageRepository();
	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-01T10:00:00Z"));

	private void say(AccountId from, AccountId to) {
		new SendMessageUseCase(directories, conversations, messages, new RecordingEventPublisher(), clock)
				.execute(new SendMessageUseCase.Command(from, to, "Olá"));
	}

	@Test
	void eachConversationShowsTheOtherPartysName() {
		AccountId ngo = directories.account(Kind.NGO, "Mãos Solidárias");
		AccountId maria = directories.account(Kind.VOLUNTEER, "Maria Souza");
		say(maria, ngo);

		List<ConversationView> inbox = new ListConversationsUseCase(conversations, messages, directories).execute(ngo);

		assertThat(inbox).singleElement().satisfies(view -> {
			assertThat(view.otherName()).isEqualTo("Maria Souza");
			assertThat(view.otherAccountId()).isEqualTo(maria);
		});
	}

	@Test
	void anInterlocutorWhoNoLongerExistsIsShownAsRemoved() {
		AccountId ngo = directories.account(Kind.NGO, "Mãos Solidárias");
		AccountId gone = directories.account(Kind.VOLUNTEER);
		say(gone, ngo);

		List<ConversationView> inbox = new ListConversationsUseCase(conversations, messages, directories).execute(ngo);

		assertThat(inbox.get(0).otherName()).isEqualTo("Conta removida");
	}
}
