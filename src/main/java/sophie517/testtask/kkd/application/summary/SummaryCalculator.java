package sophie517.testtask.kkd.application.summary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import sophie517.testtask.kkd.domain.model.Severity;

public final class SummaryCalculator {
    private final IssueDeltaCalculator deltaCalculator = new IssueDeltaCalculator();

    public Summary calculate(Snapshot current, Snapshot previous, String unitId) {
        var unit = current.unit(unitId);
        var now = scope(current, unitId);
        var before = previous != null && previous.containsUnit(unitId) ? scope(previous, unitId) : null;
        var severity = Arrays.stream(Severity.values()).map(code -> {
            long count = now.severity().getOrDefault(code, 0L);
            Long old = before == null ? null : before.severity().getOrDefault(code, 0L);
            return new Summary.SeverityCount(code, count, old, old == null ? null : count - old);
        }).toList();
        return new Summary(reference(current), previous == null ? null : reference(previous),
                new Summary.UnitReference(unit.id(), unit.name()), List.of(
                metric("clean_goals_share", "Доля корректных целей", "%", now.share(),
                        before == null ? null : before.share(), true),
                metric("goals_with_issues_count", "Количество целей с проблемами", "count",
                        BigDecimal.valueOf(now.badGoals()), before == null ? null : BigDecimal.valueOf(before.badGoals()), false),
                metric("issues_count", "Количество проблем", "count", BigDecimal.valueOf(now.issueCount()),
                        before == null ? null : BigDecimal.valueOf(before.issueCount()), false)),
                severity, before == null ? null : deltaCalculator.calculate(now.keys(), before.keys()));
    }

    private Summary.Metric metric(String key, String title, String unit, BigDecimal value,
                                  BigDecimal previous, boolean higherIsBetter) {
        BigDecimal delta = previous == null ? null : value.subtract(previous);
        String trend = null;
        if (delta != null) {
            trend = delta.signum() == 0 ? "neutral"
                    : ((delta.signum() > 0) == higherIsBetter ? "better" : "worse");
        }
        return new Summary.Metric(key, title, unit, value, previous, delta, trend);
    }

    private Scope scope(Snapshot snapshot, String unitId) {
        var units = snapshot.subtree(unitId);
        var goals = snapshot.goals().stream().filter(g -> units.contains(g.unitId()))
                .map(Snapshot.GoalData::id).collect(Collectors.toSet());
        var issues = snapshot.issues().stream().filter(i -> goals.contains(i.goalId())).toList();
        long bad = issues.stream().map(Snapshot.IssueData::goalId).distinct().count();
        BigDecimal share = goals.isEmpty() ? new BigDecimal("0.0")
                : BigDecimal.valueOf(goals.size() - bad).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(goals.size()), 1, RoundingMode.HALF_UP);
        Map<Severity, Long> severity = new EnumMap<>(Severity.class);
        issues.forEach(i -> severity.merge(i.severity(), 1L, Long::sum));
        return new Scope(share, bad, issues.size(), severity,
                issues.stream().map(Snapshot.IssueData::key).collect(Collectors.toSet()));
    }

    private Summary.RunReference reference(Snapshot snapshot) {
        return new Summary.RunReference(snapshot.id(), snapshot.date());
    }

    private record Scope(BigDecimal share, long badGoals, long issueCount,
                         Map<Severity, Long> severity, Set<Snapshot.ProblemKey> keys) { }
}
