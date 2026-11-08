package sophie517.testtask.kkd.application.service;

import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import sophie517.testtask.kkd.domain.model.GoalStatus;
import sophie517.testtask.kkd.domain.model.Issue;
import sophie517.testtask.kkd.domain.model.Run;
import sophie517.testtask.kkd.domain.model.RunGoal;
import sophie517.testtask.kkd.domain.model.RunGoalId;
import sophie517.testtask.kkd.domain.model.RunStatus;
import sophie517.testtask.kkd.domain.model.RunUnit;
import sophie517.testtask.kkd.domain.model.RunUnitId;
import sophie517.testtask.kkd.domain.model.Unit;
import sophie517.testtask.kkd.domain.repository.GoalRepository;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.KeyResultRepository;
import sophie517.testtask.kkd.domain.repository.RunGoalRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;
import sophie517.testtask.kkd.domain.repository.RunUnitRepository;
import sophie517.testtask.kkd.domain.repository.UnitRepository;
import sophie517.testtask.kkd.validation.KkdRuleEngine;
import sophie517.testtask.kkd.validation.ValidationContext;
import sophie517.testtask.kkd.validation.model.GoalData;
import sophie517.testtask.kkd.validation.model.KeyResultData;
import sophie517.testtask.kkd.validation.model.UnitData;

@Service
@RequiredArgsConstructor
public class RunService {
    private final UnitRepository units;
    private final GoalRepository goals;
    private final KeyResultRepository keyResults;
    private final RunRepository runs;
    private final RunUnitRepository runUnits;
    private final RunGoalRepository runGoals;
    private final IssueRepository issues;
    private final KkdRuleEngine engine = new KkdRuleEngine();

    /** Must be called through the Spring bean so the transaction covers reads and writes. */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public RunResult start(LocalDate snapshotDate, String previousRunId) {
        if (snapshotDate == null) throw new IllegalArgumentException("snapshotDate is required");
        if (previousRunId != null) {
            if (previousRunId.isBlank() || previousRunId.length() > 100) {
                throw new IllegalArgumentException("Invalid previousRunId");
            }
            runs.findById(previousRunId).orElseThrow(() -> new RunNotFoundException(previousRunId));
        }

        List<Unit> orderedUnits = orderUnits(units.findAll());
        var sourceGoals = goals.findAll();
        var sourceResults = keyResults.findAll();
        var unitIds = orderedUnits.stream().map(Unit::getId).collect(java.util.stream.Collectors.toSet());
        for (var goal : sourceGoals) {
            if (!unitIds.contains(goal.getUnitId())) {
                throw new IllegalArgumentException("Unknown unit: " + goal.getUnitId());
            }
        }
        var context = new ValidationContext(
                orderedUnits.stream().map(u -> new UnitData(u.getId(), u.getParentUnitId())).toList(),
                sourceGoals.stream().map(g -> new GoalData(g.getId(), g.getOwnerId(), g.getStatus(),
                        g.getUnitId(), g.getParentGoalId())).toList(),
                sourceResults.stream().map(k -> new KeyResultData(k.getGoalId(), k.getWeight())).toList());
        var violations = engine.execute(context);

        String runId = UUID.randomUUID().toString();
        runs.saveAndFlush(new Run(runId, RunStatus.COMPLETED, snapshotDate, previousRunId));
        // The parent must exist before its child's self-referencing FK is checked.
        for (Unit unit : orderedUnits) {
            runUnits.saveAndFlush(new RunUnit(new RunUnitId(runId, unit.getId()),
                    unit.getName(), unit.getParentUnitId()));
        }
        runGoals.saveAllAndFlush(sourceGoals.stream().filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                .map(g -> new RunGoal(new RunGoalId(runId, g.getId()), g.getUnitId())).toList());
        issues.saveAllAndFlush(violations.stream()
                .map(v -> new Issue(UUID.randomUUID().toString(), runId, v.ruleCode(), v.goalId(), v.severity()))
                .toList());
        return new RunResult(runId, RunStatus.COMPLETED, snapshotDate, previousRunId, violations.size());
    }

    private List<Unit> orderUnits(List<Unit> source) {
        Map<String, Unit> byId = new HashMap<>();
        for (Unit unit : source) {
            if (byId.putIfAbsent(unit.getId(), unit) != null) {
                throw new IllegalArgumentException("Duplicate unit: " + unit.getId());
            }
        }
        var roots = source.stream().filter(u -> u.getParentUnitId() == null).toList();
        if (roots.size() != 1) throw new IllegalArgumentException("Exactly one root unit is required");

        Map<String, List<Unit>> children = new HashMap<>();
        for (Unit unit : source.stream().sorted(Comparator.comparing(Unit::getId)).toList()) {
            if (unit.getParentUnitId() != null) {
                if (!byId.containsKey(unit.getParentUnitId())) {
                    throw new IllegalArgumentException("Unknown parent unit: " + unit.getParentUnitId());
                }
                children.computeIfAbsent(unit.getParentUnitId(), key -> new ArrayList<>()).add(unit);
            }
        }
        var queue = new ArrayDeque<Unit>();
        queue.add(roots.getFirst());
        List<Unit> ordered = new ArrayList<>();
        while (!queue.isEmpty()) {
            Unit unit = queue.removeFirst();
            ordered.add(unit);
            queue.addAll(children.getOrDefault(unit.getId(), List.of()));
        }
        // With one parent per unit, unreachable nodes form a disconnected cycle.
        if (ordered.size() != source.size()) throw new IllegalArgumentException("Unit hierarchy contains a cycle");
        return ordered;
    }
}
