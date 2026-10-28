package com.Unnia.social.application.usecase;

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

	public PublishPostUseCase(PostRepository posts, EventReferences eventReferences, DomainEventPublisher publisher,
			Clock clock) {
		this.posts = posts;
		this.eventReferences = eventReferences;
		this.publisher = publisher;
		this.clock = clock;
	}

	public PostId execute(Command command) {
		if (command.linkedEventId() != null && !eventReferences.exists(command.linkedEventId())) {
			throw new NotFoundException("Evento vinculado não encontrado");
		}

		Post post = Post.publish(command.authorId(), PostContent.of(command.text()),
				command.media() == null ? List.of() : command.media(), command.linkedEventId(), clock.instant());

		posts.save(post);
		publisher.publishFrom(post);
		return post.id();
	}
}
