package com.Unnia.social.application.fakes;

import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.CommentRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCommentRepository implements CommentRepository {

	private final Map<CommentId, Comment> store = new LinkedHashMap<>();

	@Override
	public Comment save(Comment comment) {
		store.put(comment.id(), comment);
		return comment;
	}

	@Override
	public Optional<Comment> findById(CommentId id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public List<Comment> findByPost(PostId postId) {
		return store.values().stream().filter(c -> c.postId().equals(postId))
				.sorted(Comparator.comparing(Comment::createdAt)).toList();
	}

	@Override
	public long countActiveByPost(PostId postId) {
		return store.values().stream().filter(c -> c.postId().equals(postId) && c.isActive()).count();
	}
}
