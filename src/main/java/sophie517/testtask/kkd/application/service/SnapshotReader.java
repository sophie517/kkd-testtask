package sophie517.testtask.kkd.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sophie517.testtask.kkd.application.summary.Snapshot;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.RunGoalRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;
import sophie517.testtask.kkd.domain.repository.RunUnitRepository;

@Service
@RequiredArgsConstructor
public class SnapshotReader {
    private final RunRepository runs;
    private final RunUnitRepository units;
    private final RunGoalRepository goals;
    private final IssueRepository issues;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Snapshot read(String runId) {
        var run = runs.findById(runId).orElseThrow(() -> new RunNotFoundException(runId));
        return new Snapshot(run.getId(), run.getSnapshotDate(), run.getPreviousRunId(),
                units.findAllByIdRunId(runId).stream().map(u -> new Snapshot.UnitData(
                        u.getId().getUnitId(), u.getName(), u.getParentUnitId())).toList(),
                goals.findAllByIdRunId(runId).stream().map(g -> new Snapshot.GoalData(
                        g.getId().getGoalId(), g.getUnitId())).toList(),
                issues.findAllByRunId(runId).stream().map(i -> new Snapshot.IssueData(
                        i.getId(), i.getRuleCode(), i.getGoalId(), i.getSeverity())).toList());
    }
}
