package com.Unnia.identity.application.usecase;

import com.Unnia.identity.application.AdminPolicy;
import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.model.AccountStatus;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.identity.domain.repository.NgoProfileRepository;
import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fila de revisão de ONGs para a equipe da plataforma: lista as ONGs em uma situação de verificação
 * (por exemplo, as pendentes), com os dados de que o administrador precisa para decidir e o estado da conta.
 */
public class ListNgosForReviewUseCase {

	public static final int MAX_PAGE_SIZE = 50;

	public record NgoReview(NgoId ngoId, AccountId accountId, String name, String cnpj, String description,
			String city, String email, VerificationStatus verificationStatus, AccountStatus accountStatus) {
	}

	private final AdminPolicy adminPolicy;
	private final NgoProfileRepository ngos;
	private final AccountRepository accounts;

	public ListNgosForReviewUseCase(AdminPolicy adminPolicy, NgoProfileRepository ngos, AccountRepository accounts) {
		this.adminPolicy = adminPolicy;
		this.ngos = ngos;
		this.accounts = accounts;
	}

	public List<NgoReview> execute(AccountId adminId, VerificationStatus status, int page, int size) {
		adminPolicy.requireAdmin(adminId);
		Guard.notNull(status, "A situação de verificação é obrigatória");
		Guard.check(page >= 0, "Página inválida");
		Guard.check(size >= 1 && size <= MAX_PAGE_SIZE, "O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE);

		List<NgoProfile> profiles = ngos.findByVerificationStatus(status, page, size);
		Map<AccountId, Account> owners = accounts.findAllById(profiles.stream().map(NgoProfile::accountId).toList())
				.stream().collect(Collectors.toMap(Account::id, Function.identity()));

		return profiles.stream().filter(profile -> owners.containsKey(profile.accountId())).map(profile -> {
			Account owner = owners.get(profile.accountId());
			return new NgoReview(profile.id(), profile.accountId(), profile.name().value(),
					profile.cnpj().formatted(), profile.description().value(), profile.city().value(),
					owner.email().value(), profile.verificationStatus(), owner.status());
		}).toList();
	}
}
