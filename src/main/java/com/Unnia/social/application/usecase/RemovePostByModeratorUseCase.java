package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.PostRepository;

/**
 * Uso interno: a autorização é feita pelo contexto de moderação antes de chamar este caso de uso.
 * Idempotente: uma postagem já removida não é erro.
 */
public class RemovePostByModeratorUseCase {

	private final PostRepository posts;
	private final DomainEventPublisher publisher;

	public RemovePostByModeratorUseCase(PostRepository posts, DomainEventPublisher publisher) {
		this.posts = posts;
		this.publisher = publisher;
	}

	public void execute(PostId postId) {
		Post post = posts.findById(postId).orElseThrow(() -> new NotFoundException("Postagem não encontrada"));
		if (!post.isActive()) {
			return;
		}

		post.removeByModerator();

		posts.save(post);
		publisher.publishFrom(post);
	}
}
