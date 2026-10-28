package com.Unnia.moderation.application.port;

import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.shared.domain.id.AccountId;

/**
 * Retira o conteúdo denunciado: remove postagem ou comentário e suspende conta. A implementação
 * delega aos casos de uso dos contextos de origem.
 */
public interface ContentTakedown {

	void takeDown(AccountId moderatorId, ReportTarget target);
}
