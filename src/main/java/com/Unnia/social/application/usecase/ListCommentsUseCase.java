package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.NotFoundException;
import com.Unnia.social.application.CommentView;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;

import java.util.List;

/** Comentários ativos da postagem, em ordem cronológica, com respostas aninhadas em um nível. */
public class ListCommentsUseCase {

	private final PostRepository posts;
	private final CommentRepository comments;

	public ListCommentsUseCase(PostRepository posts, CommentRepository comments) {
		this.posts = posts;
		this.comments = comments;
	}

	public List<CommentView> execute(PostId postId) {
		posts.findById(postId).filter(Post::isActive)
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));

		List<Comment> active = comments.findByPost(postId).stream().filter(Comment::isActive).toList();
		return active.stream().filter(c -> !c.isReply()).map(root -> CommentView.of(root, repliesOf(root.id(), active)))
				.toList();
	}

	private List<CommentView> repliesOf(CommentId rootId, List<Comment> active) {
		return active.stream().filter(c -> c.parentId().filter(rootId::equals).isPresent())
				.map(reply -> CommentView.of(reply, List.of())).toList();
	}
}
