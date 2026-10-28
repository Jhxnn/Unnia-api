package com.Unnia.moderation.application.port;

import com.Unnia.moderation.domain.model.ReportTarget;

/** Verifica se o alvo de uma denúncia existe nos contextos de origem. */
public interface ReportableContent {

	boolean exists(ReportTarget target);
}
