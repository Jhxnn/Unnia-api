package com.Unnia.social.domain.model;

import com.Unnia.shared.domain.AggregateRoot;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.domain.event.PostDeleted;
import com.Unnia.social.domain.event.PostPublished;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Post extends AggregateRoot<PostId> {

	public static final int MAX_MEDIA = 4;

	private final AccountId authorId;
	private final EventId linkedEventId;
	private final Instant createdAt;
	private final List<MediaRef> media;
	private PostContent content;
	private Instant editedAt;
	private ContentStatus status;

	private Post(PostId id, AccountId authorId, PostContent content, List<MediaRef> media, EventId linkedEventId,
			Instant createdAt, Instant editedAt, ContentStatus status) {
		super(id);
		this.authorId = Guard.notNull(authorId, "Autor é obrigatório");
		this.content = Guard.notNull(content, "Conteúdo é obrigatório");
		Guard.notNull(media, "Mídias são obrigatórias");
		Guard.check(media.size() <= MAX_MEDIA, "A postagem aceita no máximo " + MAX_MEDIA + " mídias");
		this.media = List.copyOf(media);
		this.linkedEventId = linkedEventId;
		this.createdAt = Guard.notNull(createdAt, "Data de criação é obrigatória");
		this.editedAt = editedAt;
		this.status = Guard.notNull(status, "Status é obrigatório");
	}

	public static Post publish(AccountId authorId, PostContent content, List<MediaRef> media,
			EventId linkedEventId, Instant now) {
		Post post = new Post(PostId.generate(), authorId, content, media, linkedEventId, now, null,
				ContentStatus.ACTIVE);
		post.raise(new PostPublished(post.id(), authorId));
		return post;
	}

	public static Post restore(PostId id, AccountId authorId, PostContent content, List<MediaRef> media,
			EventId linkedEventId, Instant createdAt, Instant editedAt, ContentStatus status) {
		return new Post(id, authorId, content, media, linkedEventId, createdAt, editedAt, status);
	}

	public void edit(AccountId editor, PostContent newContent, Instant now) {
		ensureAuthor(editor);
		ensureActive();
		this.content = Guard.notNull(newContent, "Conteúdo é obrigatório");
		this.editedAt = now;
	}

	public void delete(AccountId requester) {
		ensureAuthor(requester);
		ensureActive();
		status = ContentStatus.DELETED;
		raise(new PostDeleted(id()));
	}

	/** Remoção por moderação: não exige ser o autor. */
	public void removeByModerator() {
		ensureActive();
		status = ContentStatus.DELETED;
		raise(new PostDeleted(id()));
	}

	public boolean isActive() {
		return status == ContentStatus.ACTIVE;
	}

	private void ensureAuthor(AccountId requester) {
		Guard.check(authorId.equals(requester), "Somente o autor pode alterar a postagem");
	}

	private void ensureActive() {
		Guard.check(isActive(), "Postagem removida");
	}

	public AccountId authorId() {
		return authorId;
	}

	public PostContent content() {
		return content;
	}

	public List<MediaRef> media() {
		return Collections.unmodifiableList(media);
	}

	public Optional<EventId> linkedEventId() {
		return Optional.ofNullable(linkedEventId);
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Optional<Instant> editedAt() {
		return Optional.ofNullable(editedAt);
	}

	public ContentStatus status() {
		return status;
	}
}
