package com.Unnia.social.infrastructure.storage;

import com.Unnia.shared.application.ServiceUnavailableException;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.social.application.port.MediaStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;

/**
 * Guarda as imagens num bucket <strong>público</strong> do Supabase Storage. O envio usa a chave de serviço
 * ({@code service_role}), que fica só no servidor e nunca vai para o navegador; a leitura é pela URL pública do
 * bucket. Sem URL ou chave configuradas, o armazenamento fica desligado e o envio de imagens responde 503.
 */
@Component
class SupabaseMediaStorage implements MediaStorage {

	private static final Logger log = LoggerFactory.getLogger(SupabaseMediaStorage.class);
	private static final String FOLDER = "posts/";

	private final String baseUrl;
	private final String serviceKey;
	private final String bucket;
	private final RestClient client;

	@Autowired
	SupabaseMediaStorage(@Value("${unnia.supabase.url:}") String url,
			@Value("${unnia.supabase.service-key:}") String serviceKey,
			@Value("${unnia.supabase.bucket:unnia-media}") String bucket) {
		this(url, serviceKey, bucket, defaultClient());
	}

	/** Para testes: permite trocar o cliente HTTP. */
	SupabaseMediaStorage(String url, String serviceKey, String bucket, RestClient client) {
		this.baseUrl = url == null ? "" : url.trim().replaceAll("/+$", "");
		this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
		this.bucket = bucket;
		this.client = client;
	}

	private static RestClient defaultClient() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
		factory.setReadTimeout(Duration.ofSeconds(20));
		return RestClient.builder().requestFactory(factory).build();
	}

	@Override
	public boolean isEnabled() {
		return !baseUrl.isEmpty() && !serviceKey.isEmpty();
	}

	@Override
	public String store(AccountId owner, String extension, String contentType, byte[] content) {
		// um caminho por conta e nome sorteado: ninguém sobrescreve nem adivinha o arquivo de outra pessoa
		String path = FOLDER + owner.value() + "/" + UUID.randomUUID() + "." + extension;
		try {
			client.post().uri(baseUrl + "/storage/v1/object/" + bucket + "/" + path)
					.header("Authorization", "Bearer " + serviceKey).header("apikey", serviceKey)
					.header("x-upsert", "false").contentType(MediaType.parseMediaType(contentType)).body(content)
					.retrieve().toBodilessEntity();
		} catch (RestClientResponseException e) {
			// registra o status, nunca os cabeçalhos: a chave de serviço não pode ir para o log
			log.error("Supabase Storage recusou o envio: HTTP {}", e.getStatusCode().value());
			throw unavailable(e);
		} catch (RestClientException e) {
			log.error("Supabase Storage inacessível: {}", e.getClass().getSimpleName());
			throw unavailable(e);
		}
		return publicPrefix() + path;
	}

	@Override
	public boolean owns(String url) {
		return isEnabled() && url != null && url.startsWith(publicPrefix() + FOLDER);
	}

	private String publicPrefix() {
		return baseUrl + "/storage/v1/object/public/" + bucket + "/";
	}

	private static ServiceUnavailableException unavailable(Exception cause) {
		return new ServiceUnavailableException("Não foi possível enviar a imagem agora. Tente novamente em instantes.",
				cause);
	}
}
