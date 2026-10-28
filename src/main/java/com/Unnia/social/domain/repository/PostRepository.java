package com.Unnia.social.domain.repository;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.Post;
import com.Unnia.social.domain.model.PostId;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

	Post save(Post post);

	Optional<Post> findById(PostId id);

	List<Post> findByAuthor(AccountId authorId);

	/** Feed: postagens ativas dos autores informados, da mais recente para a mais antiga. */
	List<Post> findFeed(List<AccountId> authors, int page, int size);
}
