package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.domain.event.PostDeleted;
import com.Unnia.social.domain.event.PostPublished;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostUseCasesTest {

	private SocialFixture fx;
	private AccountId author;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		author = fx.account();
	}

	private PublishPostUseCase publish() {
		return new PublishPostUseCase(fx.posts, fx.directories, fx.publisher, fx.clock);
	}

	@Test
	void publishesPostAndEvent() {
		PostId id = publish().execute(new PublishPostUseCase.Command(author, "Vem aí o mutirão!",
				List.of(MediaRef.image("https://x.com/a.png")), null));

		Post post = fx.posts.findById(id).orElseThrow();
		assertThat(post.authorId()).isEqualTo(author);
		assertThat(post.content().text()).isEqualTo("Vem aí o mutirão!");
		assertThat(post.media()).hasSize(1);
		assertThat(fx.publisher.published()).containsExactly(new PostPublished(id, author));
	}

	@Test
	void linksPostToAnExistingEventOnly() {
		EventId existing = fx.directories.newEvent();

		PostId id = publish().execute(new PublishPostUseCase.Command(author, "Evento!", null, existing));

		assertThat(fx.posts.findById(id).orElseThrow().linkedEventId()).contains(existing);
		assertThatThrownBy(() -> publish().execute(
				new PublishPostUseCase.Command(author, "Evento!", null, EventId.generate())))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void rejectsInvalidContent() {
		assertThatThrownBy(() -> publish().execute(new PublishPostUseCase.Command(author, " ", null, null)))
				.isInstanceOf(DomainException.class);
		assertThat(fx.publisher.published()).isEmpty();
	}

	@Test
	void authorEditsPost() {
		PostId id = fx.post(author, "Original");
		fx.clock.advance(Duration.ofMinutes(5));

		new EditPostUseCase(fx.posts, fx.clock).execute(new EditPostUseCase.Command(author, id, "Editado"));

		Post post = fx.posts.findById(id).orElseThrow();
		assertThat(post.content().text()).isEqualTo("Editado");
		assertThat(post.editedAt()).contains(fx.clock.instant());
	}

	@Test
	void onlyTheAuthorEditsOrDeletes() {
		PostId id = fx.post(author, "Original");
		AccountId stranger = fx.account();

		assertThatThrownBy(() -> new EditPostUseCase(fx.posts, fx.clock)
				.execute(new EditPostUseCase.Command(stranger, id, "Hack"))).isInstanceOf(ForbiddenException.class);
		assertThatThrownBy(() -> new DeletePostUseCase(fx.posts, fx.publisher)
				.execute(new DeletePostUseCase.Command(stranger, id))).isInstanceOf(ForbiddenException.class);
		assertThat(fx.posts.findById(id).orElseThrow().content().text()).isEqualTo("Original");
	}

	@Test
	void deletesPostAndItBecomesNotFound() {
		PostId id = fx.post(author, "Original");

		new DeletePostUseCase(fx.posts, fx.publisher).execute(new DeletePostUseCase.Command(author, id));

		assertThat(fx.publisher.published()).containsExactly(new PostDeleted(id));
		assertThatThrownBy(() -> new DeletePostUseCase(fx.posts, fx.publisher)
				.execute(new DeletePostUseCase.Command(author, id))).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> new EditPostUseCase(fx.posts, fx.clock)
				.execute(new EditPostUseCase.Command(author, id, "x"))).isInstanceOf(NotFoundException.class);
	}

	@Test
	void unknownPostIsNotFound() {
		assertThatThrownBy(() -> new EditPostUseCase(fx.posts, fx.clock)
				.execute(new EditPostUseCase.Command(author, PostId.generate(), "x")))
				.isInstanceOf(NotFoundException.class);
	}
}
