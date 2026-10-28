package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.DomainEventPublisher;
import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.PostRepository;

public class DeletePostUseCase {

	public record Command(AccountId accountId, PostId postId) {
	}

	private final PostRepository posts;
	private final DomainEventPublisher publisher;

	public DeletePostUseCase(PostRepository posts, DomainEventPublisher publisher) {
		this.posts = posts;
		this.publisher = publisher;
	}

	public void execute(Command command) {
		Post post = posts.findById(command.postId()).filter(Post::isActive)
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));
		if (!post.authorId().equals(command.accountId())) {
			throw new ForbiddenException("Somente o autor pode remover a postagem");
		}

		post.delete(command.accountId());

		posts.save(post);
		publisher.publishFrom(post);
	}
}
