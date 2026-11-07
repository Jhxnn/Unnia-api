package com.Unnia.social.infrastructure.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.Unnia.identity.infrastructure.security.AuthenticatedUser;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.social.application.CommentView;
import com.Unnia.social.application.PostView;
import com.Unnia.social.application.usecase.AddCommentUseCase;
import com.Unnia.social.application.usecase.DeletePostUseCase;
import com.Unnia.social.application.usecase.EditPostUseCase;
import com.Unnia.social.application.usecase.GetFeedUseCase;
import com.Unnia.social.application.usecase.GetPostUseCase;
import com.Unnia.social.application.usecase.ListCommentsUseCase;
import com.Unnia.social.application.usecase.ListPostsByAuthorUseCase;
import com.Unnia.social.application.usecase.PublishPostUseCase;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.MediaRef;
import com.Unnia.social.domain.model.MediaType;
import com.Unnia.social.domain.model.PostId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Postagens")
@RestController
@RequestMapping("/api/posts")
class PostController {

	record MediaRequest(@NotBlank String url, @NotNull MediaType type) {
	}

	record PublishPostRequest(@NotBlank String text, List<@Valid MediaRequest> media, UUID linkedEventId) {
	}

	record EditPostRequest(@NotBlank String text) {
	}

	record CommentRequest(@NotBlank String text) {
	}

	record PostCreated(PostId postId) {
	}

	record CommentCreated(CommentId commentId) {
	}

	private final PublishPostUseCase publish;
	private final EditPostUseCase edit;
	private final DeletePostUseCase delete;
	private final GetPostUseCase get;
	private final GetFeedUseCase feed;
	private final ListPostsByAuthorUseCase byAuthor;
	private final AddCommentUseCase addComment;
	private final ListCommentsUseCase listComments;

	PostController(PublishPostUseCase publish, EditPostUseCase edit, DeletePostUseCase delete, GetPostUseCase get,
			GetFeedUseCase feed, ListPostsByAuthorUseCase byAuthor, AddCommentUseCase addComment,
			ListCommentsUseCase listComments) {
		this.publish = publish;
		this.edit = edit;
		this.delete = delete;
		this.get = get;
		this.feed = feed;
		this.byAuthor = byAuthor;
		this.addComment = addComment;
		this.listComments = listComments;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	PostCreated publish(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody PublishPostRequest request) {
		List<MediaRef> media = request.media() == null ? List.of()
				: request.media().stream().map(m -> new MediaRef(m.url(), m.type())).toList();
		EventId linkedEvent = request.linkedEventId() == null ? null : new EventId(request.linkedEventId());
		return new PostCreated(publish.execute(
				new PublishPostUseCase.Command(user.accountId(), request.text(), media, linkedEvent)));
	}

	@GetMapping("/feed")
	List<PostView> feed(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return feed.execute(user.accountId(), page, size);
	}

	@GetMapping("/author/{accountId}")
	List<PostView> byAuthor(@PathVariable UUID accountId) {
		return byAuthor.execute(new AccountId(accountId));
	}

	@GetMapping("/{id}")
	PostView byId(@PathVariable UUID id) {
		return get.execute(new PostId(id));
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void edit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody EditPostRequest request) {
		edit.execute(new EditPostUseCase.Command(user.accountId(), new PostId(id), request.text()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		delete.execute(new DeletePostUseCase.Command(user.accountId(), new PostId(id)));
	}

	@GetMapping("/{id}/comments")
	List<CommentView> comments(@PathVariable UUID id) {
		return listComments.execute(new PostId(id));
	}

	@PostMapping("/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	CommentCreated comment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody CommentRequest request) {
		return new CommentCreated(addComment.execute(
				new AddCommentUseCase.Command(user.accountId(), new PostId(id), request.text())));
	}
}
