package sophie517.testtask.kkd.application.service;

import java.time.LocalDate;
import sophie517.testtask.kkd.domain.model.RunStatus;

public record RunResult(String id, RunStatus status, LocalDate snapshotDate,
                        String previousRunId, int issueCount) {
}
