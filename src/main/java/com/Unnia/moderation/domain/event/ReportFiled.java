package com.Unnia.moderation.domain.event;

import com.Unnia.moderation.domain.model.ReportId;
import com.Unnia.moderation.domain.model.ReportTarget;
import com.Unnia.shared.domain.DomainEvent;

public record ReportFiled(ReportId reportId, ReportTarget target) implements DomainEvent {
}
