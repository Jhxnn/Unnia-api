package com.Unnia.social.application.usecase;

import com.Unnia.social.domain.model.MediaType;
import com.Unnia.social.application.port.MediaStorage;
import com.Unnia.shared.domain.DomainException;
import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.port.EventReferences;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostContent;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.PostRepository;

import java.time.Clock;
import java.util.List;

public class PublishPostUseCase {

	public record Command(AccountId authorId, String text, List<MediaRef> media, EventId linkedEventId) {
	}

	private final PostRepository posts;
	private final EventReferences eventReferences;
	private final DomainEventPublisher publisher;
	private final Clock clock;
	private final MediaStorage mediaStorage;

	public PublishPostUseCase(PostRepository posts, EventReferences eventReferences, DomainEventPublisher publisher,
			Clock clock, MediaStorage mediaStorage) {
		this.posts = posts;
		this.eventReferences = eventReferences;
		this.publisher = publisher;
		this.clock = clock;
		this.mediaStorage = mediaStorage;
	}

	public PostId execute(Command command) {
		if (command.linkedEventId() != null && !eventReferences.exists(command.linkedEventId())) {
			throw new NotFoundException("Evento vinculado não encontrado");
		}

		List<MediaRef> media = command.media() == null ? List.of() : command.media();
		for (MediaRef item : media) {
			// só vale mídia que passou pelo envio do Unnia: nada de imagem de terceiros nem de outro tipo
			if (item.type() != MediaType.IMAGE || !mediaStorage.owns(item.url())) {
				throw new DomainException("Imagem inválida: envie a imagem pelo Unnia");
			}
		}
		Post post = Post.publish(command.authorId(), PostContent.of(command.text()),
				media, command.linkedEventId(), clock.instant());

		posts.save(post);
		publisher.publishFrom(post);
		return post.id();
	}
}
