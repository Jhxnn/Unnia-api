package com.Unnia.moderation.domain.event;

import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.shared.domain.DomainEvent;

public record ReportDismissed(ReportId reportId) implements DomainEvent {
}
