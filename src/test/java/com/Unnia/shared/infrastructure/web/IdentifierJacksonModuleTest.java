package com.Unnia.shared.infrastructure.web;

import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.domain.model.PostId;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierJacksonModuleTest {

	record Payload(AccountId account, PostId post, List<AccountId> others) {
	}

	private final JsonMapper mapper = JsonMapper.builder().addModule(new IdentifierJacksonModule()).build();

	@Test
	void serializesIdsAsPlainStrings() {
		UUID account = UUID.randomUUID();
		UUID post = UUID.randomUUID();
		UUID other = UUID.randomUUID();

		String json = mapper.writeValueAsString(new Payload(new AccountId(account), new PostId(post),
				List.of(new AccountId(other))));

		assertThat(json).contains("\"account\":\"" + account + "\"");
		assertThat(json).contains("\"post\":\"" + post + "\"");
		assertThat(json).contains("\"others\":[\"" + other + "\"]");
		assertThat(json).doesNotContain("\"value\"");
	}

	@Test
	void serializesNullIdsAsNull() {
		String json = mapper.writeValueAsString(new Payload(null, null, List.of()));

		assertThat(json).contains("\"account\":null");
	}
}
