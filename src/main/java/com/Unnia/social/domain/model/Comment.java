package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.event.CommentDeleted;
import com.Unnia.social.domain.event.CommentPosted;

import java.time.Instant;
import java.util.Optional;

public class Comment extends AggregateRoot<CommentId> {

	private final PostId postId;
	private final CommentId parentId;
	private final AccountId authorId;
	private final Instant createdAt;
	private CommentContent content;
	private ContentStatus status;

	private Comment(CommentId id, PostId postId, CommentId parentId, AccountId authorId, CommentContent content,
			Instant createdAt, ContentStatus status) {
		super(id);
		this.postId = Guard.notNull(postId, "Postagem é obrigatória");
		this.parentId = parentId;
		this.authorId = Guard.notNull(authorId, "Autor é obrigatório");
		this.content = Guard.notNull(content, "Conteúdo é obrigatório");
		this.createdAt = Guard.notNull(createdAt, "Data de criação é obrigatória");
		this.status = Guard.notNull(status, "Status é obrigatório");
	}

	public static Comment onPost(Post post, AccountId authorId, CommentContent content, Instant now) {
		Guard.notNull(post, "Postagem é obrigatória");
		Guard.check(post.isActive(), "Não é possível comentar uma postagem removida");
		Comment comment = new Comment(CommentId.generate(), post.id(), null, authorId, content, now,
				ContentStatus.ACTIVE);
		comment.raise(new CommentPosted(comment.id(), post.id(), authorId));
		return comment;
	}

	/** Respostas têm um único nível: não é possível responder a uma resposta. */
	public static Comment replyTo(Comment parent, AccountId authorId, CommentContent content, Instant now) {
		Guard.notNull(parent, "Comentário pai é obrigatório");
		Guard.check(parent.isActive(), "Não é possível responder a um comentário removido");
		Guard.check(!parent.isReply(), "Não é possível responder a uma resposta");
		Comment reply = new Comment(CommentId.generate(), parent.postId, parent.id(), authorId, content, now,
				ContentStatus.ACTIVE);
		reply.raise(new CommentPosted(reply.id(), parent.postId, authorId));
		return reply;
	}

	public static Comment restore(CommentId id, PostId postId, CommentId parentId, AccountId authorId,
			CommentContent content, Instant createdAt, ContentStatus status) {
		return new Comment(id, postId, parentId, authorId, content, createdAt, status);
	}

	public void delete(AccountId requester) {
		Guard.check(authorId.equals(requester), "Somente o autor pode remover o comentário");
		Guard.check(isActive(), "Comentário já removido");
		status = ContentStatus.DELETED;
		raise(new CommentDeleted(id()));
	}

	public boolean isActive() {
		return status == ContentStatus.ACTIVE;
	}

	public boolean isReply() {
		return parentId != null;
	}

	public PostId postId() {
		return postId;
	}

	public Optional<CommentId> parentId() {
		return Optional.ofNullable(parentId);
	}

	public AccountId authorId() {
		return authorId;
	}

	public CommentContent content() {
		return content;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public ContentStatus status() {
		return status;
	}
}
