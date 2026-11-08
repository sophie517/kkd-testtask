package sophie517.testtask.kkd.application.service;

import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sophie517.testtask.kkd.application.summary.Snapshot;
import sophie517.testtask.kkd.domain.model.Issue;
import sophie517.testtask.kkd.domain.model.Run;
import sophie517.testtask.kkd.domain.model.RunStatus;
import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
@RequiredArgsConstructor
public class KkdQueryService {
    private final RunRepository runs;
    private final IssueRepository issues;
    private final SnapshotReader snapshots;

    public record RunItem(String id, RunStatus status, LocalDate snapshotDate,
                          String comparedToRunId, long issueCount) { }
    public record IssueItem(String id, String runId, String ruleCode, Severity severity,
                            String goalId, LocalDate snapshotDate) { }
    public record IssuePage(List<IssueItem> items, int total, int limit, int offset) { }

    public List<RunItem> listRuns() {
        Map<String, Long> counts = issues.findAll().stream()
                .collect(Collectors.groupingBy(Issue::getRunId, Collectors.counting()));
        return runs.findAll().stream().sorted(Comparator.comparing(Run::getSnapshotDate).reversed()
                        .thenComparing(Run::getId))
                .map(r -> new RunItem(r.getId(), r.getStatus(), r.getSnapshotDate(),
                        r.getPreviousRunId(), counts.getOrDefault(r.getId(), 0L))).toList();
    }

    public IssuePage listIssues(String runId, String unitId, String ruleCode, Severity severity, int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0) throw new IllegalArgumentException("Invalid pagination");
        Snapshot snapshot = snapshots.read(runId);
        var units = unitId == null ? null : snapshot.subtree(unitId);
        var goalUnits = snapshot.goals().stream().collect(Collectors.toMap(Snapshot.GoalData::id, Snapshot.GoalData::unitId));
        var selected = snapshot.issues().stream()
                .filter(i -> units == null || units.contains(goalUnits.get(i.goalId())))
                .filter(i -> ruleCode == null || ruleCode.equals(i.ruleCode()))
                .filter(i -> severity == null || severity == i.severity())
                .sorted(Comparator.comparing(Snapshot.IssueData::ruleCode).thenComparing(Snapshot.IssueData::goalId))
                .toList();
        int start = Math.min(offset, selected.size());
        int end = (int) Math.min((long) offset + limit, selected.size());
        var page = selected.subList(start, end).stream().map(i -> new IssueItem(i.id(), snapshot.id(),
                i.ruleCode(), i.severity(), i.goalId(), snapshot.date())).toList();
        return new IssuePage(page, selected.size(), limit, offset);
    }
}
