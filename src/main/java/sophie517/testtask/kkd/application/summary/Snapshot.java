package sophie517.testtask.kkd.application.summary;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sophie517.testtask.kkd.application.service.UnitNotFoundException;
import sophie517.testtask.kkd.domain.model.Severity;

/** Independent historical input; no persistence entities or current data. */
public record Snapshot(String id, LocalDate date, String previousRunId,
                       List<UnitData> units, List<GoalData> goals, List<IssueData> issues) {
    public Snapshot {
        units = List.copyOf(units);
        goals = List.copyOf(goals);
        issues = List.copyOf(issues);
    }

    public record UnitData(String id, String name, String parentUnitId) { }
    public record GoalData(String id, String unitId) { }
    public record ProblemKey(String ruleCode, String goalId) { }
    public record IssueData(String id, String ruleCode, String goalId, Severity severity) {
        public ProblemKey key() { return new ProblemKey(ruleCode, goalId); }
    }

    public UnitData unit(String unitId) {
        return units.stream().filter(u -> u.id().equals(unitId)).findFirst()
                .orElseThrow(() -> new UnitNotFoundException(unitId));
    }

    public boolean containsUnit(String unitId) {
        return units.stream().anyMatch(u -> u.id().equals(unitId));
    }

    public Set<String> subtree(String unitId) {
        unit(unitId);
        Set<String> result = new HashSet<>();
        var queue = new ArrayDeque<String>();
        queue.add(unitId);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!result.add(current)) throw new IllegalStateException("Cycle in saved unit hierarchy");
            units.stream().filter(u -> current.equals(u.parentUnitId())).map(UnitData::id).forEach(queue::addLast);
        }
        return Set.copyOf(result);
    }
}
