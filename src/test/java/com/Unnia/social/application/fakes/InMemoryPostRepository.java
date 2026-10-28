package com.Unnia.social.application.fakes;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.PostRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryPostRepository implements PostRepository {

	private final Map<PostId, Post> store = new LinkedHashMap<>();

	@Override
	public Post save(Post post) {
		store.put(post.id(), post);
		return post;
	}

	@Override
	public Optional<Post> findById(PostId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Post> findByAuthor(AccountId authorId) {
		return store.values().stream().filter(p -> p.authorId().equals(authorId)).toList();
	}

	@Override
	public List<Post> findFeed(List<AccountId> authors, int page, int size) {
		return store.values().stream()
				.filter(p -> p.isActive() && authors.contains(p.authorId()))
				.sorted(Comparator.comparing(Post::createdAt).reversed())
				.skip((long) page * size)
				.limit(size)
				.toList();
	}
}
