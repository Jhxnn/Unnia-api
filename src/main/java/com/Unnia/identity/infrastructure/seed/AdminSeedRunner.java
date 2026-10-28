package com.Unnia.identity.infrastructure.seed;

import com.Unnia.identity.application.usecase.RegisterAdminUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador da plataforma na inicialização. Idempotente: se o admin já existe, não faz nada.
 * Sem senha configurada, ou sem o caso de uso disponível, apenas registra um aviso.
 */
@Component
public class AdminSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

	private final ObjectProvider<RegisterAdminUseCase> registerAdmin;
	private final String email;
	private final String password;

	public AdminSeedRunner(ObjectProvider<RegisterAdminUseCase> registerAdmin,
			@Value("${unnia.admin.email:}") String email, @Value("${unnia.admin.password:}") String password) {
		this.registerAdmin = registerAdmin;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank()) {
			log.warn("Seed do administrador ignorado: defina UNNIA_ADMIN_EMAIL e UNNIA_ADMIN_PASSWORD");
			return;
		}
		RegisterAdminUseCase useCase = registerAdmin.getIfAvailable();
		if (useCase == null) {
			log.warn("Seed do administrador ignorado: RegisterAdminUseCase ainda não está configurado como bean");
			return;
		}
		RegisterAdminUseCase.Result result = useCase.execute(new RegisterAdminUseCase.Command(email, password));
		log.info(result.created() ? "Administrador criado: {}" : "Administrador já existia: {}", email);
	}
}
