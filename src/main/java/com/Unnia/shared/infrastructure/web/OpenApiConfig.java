package com.Unnia.shared.infrastructure.web;

import com.Unnia.events.domain.model.RegistrationId;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.messaging.domain.model.NotificationId;
import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.FollowId;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.ReactionId;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;

/** Documentação OpenAPI: {@code /v3/api-docs} (JSON) e {@code /swagger-ui.html} (interface). */
@Configuration
class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	static {
		// Os ids viajam como string UUID (ver IdentifierJacksonModule); sem isto a documentação
		// os descreveria como objetos {"value": "..."}.
		List.of(AccountId.class, NgoId.class, VolunteerId.class, EventId.class, RegistrationId.class, PostId.class,
				CommentId.class, ReactionId.class, FollowId.class, ConversationId.class, MessageId.class,
				NotificationId.class, ReportId.class)
				.forEach(id -> SpringDocUtils.getConfig().replaceWithClass(id, UUID.class));
	}

	@Bean
	OpenAPI unniaOpenApi() {
		return new OpenAPI()
				.info(new Info().title("Unnia API").version("v1")
						.description("Rede social para ONGs divulgarem eventos solidários e voluntários se "
								+ "inscreverem neles. Autentique-se em POST /api/auth/login e envie o token em "
								+ "Authorization: Bearer. As rotas públicas são cadastro, login, listagem e "
								+ "consulta de eventos e perfis; as demais exigem token, e /api/admin/** exige o "
								+ "papel ADMIN."))
				.components(new Components().addSecuritySchemes(BEARER,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}
}
