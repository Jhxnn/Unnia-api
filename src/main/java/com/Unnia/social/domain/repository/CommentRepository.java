package com.Unnia.social.domain.repository;

import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

	Comment save(Comment comment);

	Optional<Comment> findById(CommentId id);

	List<Comment> findByPost(PostId postId);
}
