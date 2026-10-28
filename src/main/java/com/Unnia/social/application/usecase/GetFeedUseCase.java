package com.Unnia.social.application.usecase;

import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.PostView;
import com.Unnia.social.application.PostViews;
import com.Unnia.social.domain.model.Follow;
import com.Unnia.social.domain.repository.FollowRepository;
import com.Unnia.social.domain.repository.PostRepository;

import java.util.ArrayList;
import java.util.List;

/** Feed pessoal: postagens da própria conta e das contas que ela segue, da mais recente para a mais antiga. */
public class GetFeedUseCase {

	public static final int MAX_PAGE_SIZE = 50;

	private final FollowRepository follows;
	private final PostRepository posts;
	private final PostViews views;

	public GetFeedUseCase(FollowRepository follows, PostRepository posts, PostViews views) {
		this.follows = follows;
		this.posts = posts;
		this.views = views;
	}

	public List<PostView> execute(AccountId accountId, int page, int size) {
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);

		List<AccountId> authors = new ArrayList<>();
		authors.add(accountId);
		follows.findByFollower(accountId).stream().map(Follow::followeeId).forEach(authors::add);

		return posts.findFeed(authors, page, size).stream().map(views::of).toList();
	}
}
