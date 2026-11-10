package com.Unnia.social.infrastructure.storage;

import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.shared.domain.id.AccountId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseMediaStorageTest {

	private static final String URL = "https://abcd.supabase.co";
	private static final String KEY = "chave-de-servico-secreta";

	private MockRestServiceServer server;
	private SupabaseMediaStorage storage;
	private final AccountId owner = AccountId.generate();

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		storage = new SupabaseMediaStorage(URL + "/", KEY, "unnia-media", builder.build());
	}

	@Test
	void uploadsToTheBucketWithTheServiceKeyAndReturnsThePublicUrl() {
		byte[] content = { 1, 2, 3 };
		server.expect(requestTo(org.hamcrest.Matchers.startsWith(URL + "/storage/v1/object/unnia-media/posts/" + owner.value() + "/")))
				.andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer " + KEY))
				.andExpect(header("apikey", KEY)).andExpect(header("x-upsert", "false"))
				.andExpect(header("Content-Type", "image/png")).andExpect(content().bytes(content))
				.andRespond(withSuccess("{\"Key\":\"x\"}", org.springframework.http.MediaType.APPLICATION_JSON));

		String url = storage.store(owner, "png", "image/png", content);

		assertThat(url).startsWith(URL + "/storage/v1/object/public/unnia-media/posts/" + owner.value() + "/")
				.endsWith(".png");
		server.verify();
	}

	@Test
	void eachUploadGetsADifferentRandomName() {
		server.expect(org.springframework.test.web.client.ExpectedCount.twice(),
				requestTo(org.hamcrest.Matchers.containsString("/storage/v1/object/unnia-media/")))
				.andRespond(withSuccess());

		String first = storage.store(owner, "jpg", "image/jpeg", new byte[] { 1 });
		String second = storage.store(owner, "jpg", "image/jpeg", new byte[] { 1 });

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void aRefusalFromSupabaseBecomesServiceUnavailableWithoutLeakingTheKey() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/storage/v1/object/")))
				.andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"message\":\"Invalid key\"}"));

		assertThatThrownBy(() -> storage.store(owner, "png", "image/png", new byte[] { 1 }))
				.isInstanceOf(ServiceUnavailableException.class)
				.hasMessage("Não foi possível enviar a imagem agora. Tente novamente em instantes.")
				.satisfies(error -> assertThat(String.valueOf(error.getMessage())).doesNotContain(KEY));
	}

	@Test
	void aServerErrorAlsoBecomesServiceUnavailable() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/storage/v1/object/")))
				.andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertThatThrownBy(() -> storage.store(owner, "png", "image/png", new byte[] { 1 }))
				.isInstanceOf(ServiceUnavailableException.class);
	}

	@Test
	void ownsOnlyUrlsFromItsOwnBucketPostsFolder() {
		String prefix = URL + "/storage/v1/object/public/unnia-media/posts/";

		assertThat(storage.owns(prefix + owner.value() + "/a.png")).isTrue();
		assertThat(storage.owns(URL + "/storage/v1/object/public/outro-bucket/posts/a.png")).isFalse();
		assertThat(storage.owns(URL + "/storage/v1/object/public/unnia-media/avatars/a.png")).isFalse();
		assertThat(storage.owns("https://evil.example.com/storage/v1/object/public/unnia-media/posts/a.png")).isFalse();
		assertThat(storage.owns(null)).isFalse();
	}

	@Test
	void isEnabledOnlyWithBothTheUrlAndTheKey() {
		RestClient client = RestClient.create();

		assertThat(new SupabaseMediaStorage(URL, KEY, "b", client).isEnabled()).isTrue();
		assertThat(new SupabaseMediaStorage("", KEY, "b", client).isEnabled()).isFalse();
		assertThat(new SupabaseMediaStorage(URL, "", "b", client).isEnabled()).isFalse();
		assertThat(new SupabaseMediaStorage(null, null, "b", client).isEnabled()).isFalse();
	}

	@Test
	void whenDisabledItOwnsNothing() {
		assertThat(new SupabaseMediaStorage("", "", "unnia-media", RestClient.create())
				.owns("/storage/v1/object/public/unnia-media/posts/a.png")).isFalse();
	}
}
