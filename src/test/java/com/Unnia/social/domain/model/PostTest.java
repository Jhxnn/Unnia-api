package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.domain.event.PostDeleted;
import com.Unnia.social.domain.event.PostPublished;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
	private static final AccountId AUTHOR = AccountId.generate();

	private Post post() {
		return Post.publish(AUTHOR, PostContent.of("Vem aí o mutirão!"), List.of(), null, NOW);
	}

	@Test
	void publishesActivePostAndRaisesEvent() {
		Post post = post();

		assertThat(post.authorId()).isEqualTo(AUTHOR);
		assertThat(post.isActive()).isTrue();
		assertThat(post.createdAt()).isEqualTo(NOW);
		assertThat(post.editedAt()).isEmpty();
		assertThat(post.linkedEventId()).isEmpty();
		assertThat(post.pullDomainEvents()).containsExactly(new PostPublished(post.id(), AUTHOR));
	}

	@Test
	void canBeLinkedToAnEventAndCarryMedia() {
		EventId eventId = EventId.generate();

		Post post = Post.publish(AUTHOR, PostContent.of("Foto"), List.of(MediaRef.image("https://x.com/a.png")),
				eventId, NOW);

		assertThat(post.linkedEventId()).contains(eventId);
		assertThat(post.media()).containsExactly(MediaRef.image("https://x.com/a.png"));
	}

	@Test
	void acceptsAtMostFourMedia() {
		List<MediaRef> five = java.util.stream.IntStream.range(0, 5)
				.mapToObj(i -> MediaRef.image("https://x.com/" + i + ".png")).toList();

		assertThatThrownBy(() -> Post.publish(AUTHOR, PostContent.of("x"), five, null, NOW))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void contentIsRequiredAndLimited() {
		assertThatThrownBy(() -> PostContent.of(" ")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> PostContent.of("x".repeat(3001))).isInstanceOf(DomainException.class);
	}

	@Test
	void mediaMustHaveHttpUrl() {
		assertThatThrownBy(() -> MediaRef.image("ftp://x.com/a.png")).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> MediaRef.video("")).isInstanceOf(DomainException.class);
	}

	@Test
	void authorEditsAndEditTimeIsRecorded() {
		Post post = post();
		Instant later = NOW.plusSeconds(60);

		post.edit(AUTHOR, PostContent.of("Atualizado"), later);

		assertThat(post.content().text()).isEqualTo("Atualizado");
		assertThat(post.editedAt()).contains(later);
	}

	@Test
	void onlyAuthorCanEditOrDelete() {
		Post post = post();
		AccountId stranger = AccountId.generate();

		assertThatThrownBy(() -> post.edit(stranger, PostContent.of("hack"), NOW)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> post.delete(stranger)).isInstanceOf(DomainException.class);
	}

	@Test
	void deletedPostCannotBeEditedOrDeletedAgain() {
		Post post = post();
		post.pullDomainEvents();

		post.delete(AUTHOR);

		assertThat(post.isActive()).isFalse();
		assertThat(post.pullDomainEvents()).containsExactly(new PostDeleted(post.id()));
		assertThatThrownBy(() -> post.delete(AUTHOR)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> post.edit(AUTHOR, PostContent.of("x"), NOW)).isInstanceOf(DomainException.class);
	}

	@Test
	void exposedMediaIsReadOnly() {
		assertThatThrownBy(() -> post().media().add(MediaRef.image("https://x.com/a.png")))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
