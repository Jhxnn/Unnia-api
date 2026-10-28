package com.Unnia.shared.infrastructure;

import com.Unnia.events.application.EventOwnership;
import com.Unnia.events.application.port.NgoDirectory;
import com.Unnia.events.domain.repository.EventRepository;
import com.Unnia.identity.application.AdminPolicy;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.domain.repository.NotificationRepository;
import com.Unnia.moderation.application.ModeratorPolicy;
import com.Unnia.moderation.application.port.ModeratorDirectory;
import com.Unnia.social.application.PostViews;
import com.Unnia.social.domain.repository.CommentRepository;
import com.Unnia.social.domain.repository.ReactionRepository;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.time.Clock;

/**
 * Fiação da camada de aplicação. Casos de uso e handlers não têm anotações de framework: são
 * descobertos por convenção de pacote e nome, e os colaboradores simples são declarados aqui.
 */
@Configuration
@ComponentScan(basePackages = "com.Unnia", useDefaultFilters = false, includeFilters = @ComponentScan.Filter(
		type = FilterType.REGEX,
		pattern = "com\\.Unnia\\.[a-z]+\\.application\\.(usecase\\.\\w+UseCase|handler\\.\\w+Handler)"))
class ApplicationConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	static BeanPostProcessor useCaseTransactionPostProcessor() {
		return new UseCaseTransactionPostProcessor();
	}

	@Bean
	AdminPolicy adminPolicy(AccountRepository accounts) {
		return new AdminPolicy(accounts);
	}

	@Bean
	ModeratorPolicy moderatorPolicy(ModeratorDirectory moderators) {
		return new ModeratorPolicy(moderators);
	}

	@Bean
	EventOwnership eventOwnership(NgoDirectory ngos, EventRepository events) {
		return new EventOwnership(ngos, events);
	}

	@Bean
	Notifier notifier(NotificationRepository notifications, Clock clock) {
		return new Notifier(notifications, clock);
	}

	@Bean
	PostViews postViews(ReactionRepository reactions, CommentRepository comments) {
		return new PostViews(reactions, comments);
	}
}
