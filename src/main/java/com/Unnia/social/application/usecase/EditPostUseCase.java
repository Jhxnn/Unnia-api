package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.ForbiddenException;
import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostContent;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.PostRepository;

import java.time.Clock;

public class EditPostUseCase {

	public record Command(AccountId accountId, PostId postId, String text) {
	}

	private final PostRepository posts;
	private final Clock clock;

	public EditPostUseCase(PostRepository posts, Clock clock) {
		this.posts = posts;
		this.clock = clock;
	}

	public void execute(Command command) {
		Post post = posts.findById(command.postId()).filter(Post::isActive)
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));
		if (!post.authorId().equals(command.accountId())) {
			throw new ForbiddenException("Somente o autor pode editar a postagem");
		}

		post.edit(command.accountId(), PostContent.of(command.text()), clock.instant());

		posts.save(post);
	}
}
