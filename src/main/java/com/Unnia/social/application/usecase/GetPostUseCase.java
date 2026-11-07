package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.PostView;
import com.Unnia.social.application.PostViews;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.PostRepository;

public class GetPostUseCase {

	private final PostRepository posts;
	private final PostViews views;

	public GetPostUseCase(PostRepository posts, PostViews views) {
		this.posts = posts;
		this.views = views;
	}

	public PostView execute(PostId postId, AccountId viewer) {
		return posts.findById(postId).filter(Post::isActive).map(post -> views.of(post, viewer))
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));
	}
}
