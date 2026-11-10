package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.SocialFixture;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.MediaType;
import com.Unnia.social.domain.model.PostId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uma postagem só aceita imagens que passaram pelo envio do próprio Unnia. */
class PostMediaTest {

	private SocialFixture fx;
	private AccountId author;

	@BeforeEach
	void setUp() {
		fx = new SocialFixture();
		author = fx.account();
	}

	private PostId publish(List<MediaRef> media) {
		return new PublishPostUseCase(fx.posts, fx.directories, fx.publisher, fx.clock, fx.media)
				.execute(new PublishPostUseCase.Command(author, "Olha só", media, null));
	}

	@Test
	void aPostCanCarryImagesFromOurStorage() {
		String url = fx.media.knownUrl();

		PostId id = publish(List.of(MediaRef.image(url)));

		assertThat(fx.posts.findById(id).orElseThrow().media()).containsExactly(MediaRef.image(url));
	}

	@Test
	void anImageFromSomewhereElseIsRefused() {
		assertThatThrownBy(() -> publish(List.of(MediaRef.image("https://evil.example.com/rastreador.png"))))
				.isInstanceOf(DomainException.class).hasMessage("Imagem inválida: envie a imagem pelo Unnia");
	}

	@Test
	void aUrlThatOnlyLooksLikeOursIsRefused() {
		assertThatThrownBy(() -> publish(List.of(MediaRef.image("https://cdn.test/unnia-media-outro/posts/x.png"))))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void videoIsNotSupportedYet() {
		assertThatThrownBy(() -> publish(List.of(new MediaRef(fx.media.knownUrl(), MediaType.VIDEO))))
				.isInstanceOf(DomainException.class);
	}

	@Test
	void oneBadImageSpoilsTheWholePostAndNothingIsSaved() {
		assertThatThrownBy(() -> publish(List.of(MediaRef.image(fx.media.knownUrl()), MediaRef.image("https://x.com/a.png"))))
				.isInstanceOf(DomainException.class);

		assertThat(fx.posts.findByAuthor(author)).isEmpty();
	}

	@Test
	void withoutConfiguredStorageNoMediaIsAccepted() {
		fx.media.disable();

		assertThatThrownBy(() -> publish(List.of(MediaRef.image(fx.media.knownUrl())))).isInstanceOf(DomainException.class);
	}

	@Test
	void postsWithoutMediaAreUnaffected() {
		assertThat(publish(null)).isNotNull();
		assertThat(publish(List.of())).isNotNull();
	}
}
