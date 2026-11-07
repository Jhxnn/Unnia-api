package com.Unnia.identity.domain.repository;

import com.Unnia.identity.domain.model.Cnpj;
import com.Unnia.identity.domain.model.NgoProfile;
import com.Unnia.identity.domain.model.VerificationStatus;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.NgoId;

import java.util.List;
import java.util.Optional;

public interface NgoProfileRepository {

	NgoProfile save(NgoProfile profile);

	Optional<NgoProfile> findById(NgoId id);

	Optional<NgoProfile> findByAccountId(AccountId accountId);

	boolean existsByCnpj(Cnpj cnpj);

	/** ONGs na situação informada, em ordem alfabética, paginadas. */
	List<NgoProfile> findByVerificationStatus(VerificationStatus status, int page, int size);
}
