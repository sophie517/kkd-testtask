package sophie517.testtask.kkd.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sophie517.testtask.kkd.application.summary.Summary;
import sophie517.testtask.kkd.application.summary.SummaryCalculator;

@Service
@RequiredArgsConstructor
public class KkdSummaryService {
    private final SnapshotReader snapshots;
    private final SummaryCalculator calculator = new SummaryCalculator();

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Summary getSummary(String runId, String unitId) {
        var current = snapshots.read(runId);
        current.unit(unitId);
        var previous = current.previousRunId() == null ? null : snapshots.read(current.previousRunId());
        return calculator.calculate(current, previous, unitId);
    }
}
