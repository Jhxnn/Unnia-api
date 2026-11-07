package com.Unnia.social.application.usecase;

import com.Unnia.shared.application.NotFoundException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.CommentView;
import com.Unnia.social.application.port.AuthorDirectory;
import com.Unnia.social.application.port.AuthorDirectory.Author;
import com.Unnia.social.domain.model.Comment;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.PostRepository;

import java.util.List;
import java.util.Map;

/** Comentários ativos da postagem, em ordem cronológica, com respostas aninhadas em um nível. */
public class ListCommentsUseCase {

	private final PostRepository posts;
	private final CommentRepository comments;
	private final AuthorDirectory authors;

	public ListCommentsUseCase(PostRepository posts, CommentRepository comments, AuthorDirectory authors) {
		this.posts = posts;
		this.comments = comments;
		this.authors = authors;
	}

	public List<CommentView> execute(PostId postId) {
		posts.findById(postId).filter(Post::isActive)
				.orElseThrow(() -> new NotFoundException("Postagem não encontrada"));
		List<Comment> active = comments.findByPost(postId).stream().filter(Comment::isActive).toList();
		Map<AccountId, Author> found = authors.authorsOf(active.stream().map(Comment::authorId).distinct().toList());
		return active.stream().filter(c -> !c.isReply())
				.map(root -> CommentView.of(root, authorOf(root, found), repliesOf(root.id(), active, found))).toList();
	}

	private List<CommentView> repliesOf(CommentId rootId, List<Comment> active, Map<AccountId, Author> found) {
		return active.stream().filter(c -> c.parentId().filter(rootId::equals).isPresent())
				.map(reply -> CommentView.of(reply, authorOf(reply, found), List.of())).toList();
	}

	private static Author authorOf(Comment comment, Map<AccountId, Author> found) {
		return found.getOrDefault(comment.authorId(), AuthorDirectory.UNKNOWN);
	}
}
