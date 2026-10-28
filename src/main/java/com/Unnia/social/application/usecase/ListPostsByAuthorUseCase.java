package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.PostView;
import com.Unnia.social.application.PostViews;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.repository.PostRepository;

import java.util.Comparator;
import java.util.List;

/** Postagens ativas de uma conta (perfil), da mais recente para a mais antiga. */
public class ListPostsByAuthorUseCase {

	private final PostRepository posts;
	private final PostViews views;

	public ListPostsByAuthorUseCase(PostRepository posts, PostViews views) {
		this.posts = posts;
		this.views = views;
	}

	public List<PostView> execute(AccountId authorId) {
		return posts.findByAuthor(authorId).stream().filter(Post::isActive)
				.sorted(Comparator.comparing(Post::createdAt).reversed()).map(views::of).toList();
	}
}
