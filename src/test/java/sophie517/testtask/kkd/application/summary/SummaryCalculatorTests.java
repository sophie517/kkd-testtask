package sophie517.testtask.kkd.application.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import sophie517.testtask.kkd.application.service.UnitNotFoundException;
import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.support.DemoSnapshots;

class SummaryCalculatorTests {
    private final SummaryCalculator calculator = new SummaryCalculator();
    private final List<Snapshot.UnitData> tree = List.of(new Snapshot.UnitData("HQ", "Root", null),
            new Snapshot.UnitData("A", "A", "HQ"), new Snapshot.UnitData("B", "B", "HQ"));

    @Test
    void demoSummaryHasCorrectMetricsSeverityAndDelta() throws Exception {
        var previous = DemoSnapshots.load("run-001");
        var current = DemoSnapshots.load("run-002");
        var summary = calculator.calculate(current, previous, "HQ");
        assertThat(summary.metrics().get(0).value()).isEqualByComparingTo("66.7");
        assertThat(summary.metrics().get(0).previous()).isEqualByComparingTo("61.9");
        assertThat(summary.metrics().get(0).delta()).isEqualByComparingTo("4.8");
        assertThat(summary.metrics()).extracting(Summary.Metric::trend).containsOnly("better");
        assertThat(summary.metrics().get(1).value()).isEqualByComparingTo("7");
        assertThat(summary.metrics().get(2).value()).isEqualByComparingTo("8");
        assertThat(summary.issuesBySeverity()).extracting(Summary.SeverityCount::current).containsExactly(3L, 5L, 0L);
        assertThat(summary.delta()).isEqualTo(new Summary.IssueDelta(4, 4, 5));
        assertThat(calculator.calculate(previous, current, "HQ").metrics())
                .extracting(Summary.Metric::trend).containsOnly("worse");
        assertThat(calculator.calculate(current, previous, "UNIT-A").metrics().get(2).value()).isEqualByComparingTo("2");
    }

    @Test
    void firstRunHasExplicitlyMissingComparison() throws Exception {
        var summary = calculator.calculate(DemoSnapshots.load("run-001"), null, "HQ");
        assertThat(summary.comparedTo()).isNull();
        assertThat(summary.delta()).isNull();
        summary.metrics().forEach(m -> {
            assertThat(m.previous()).isNull();
            assertThat(m.delta()).isNull();
            assertThat(m.trend()).isNull();
        });
        summary.issuesBySeverity().forEach(s -> {
            assertThat(s.previous()).isNull();
            assertThat(s.delta()).isNull();
        });
    }

    @Test
    void countsDistinctGoalsAndRoundsHalfUp() {
        var goals = IntStream.range(0, 16).mapToObj(i -> new Snapshot.GoalData("G" + i, "HQ")).toList();
        var problems = new java.util.ArrayList<>(IntStream.range(0, 15)
                .mapToObj(i -> new Snapshot.IssueData("I" + i, "GOAL_NO_OWNER", "G" + i, Severity.CRITICAL)).toList());
        problems.add(new Snapshot.IssueData("extra", "GOAL_NO_KR", "G0", Severity.CRITICAL));
        var summary = calculator.calculate(snapshot(tree, goals, problems), null, "HQ");
        assertThat(summary.metrics().get(0).value()).isEqualByComparingTo("6.3");
        assertThat(summary.metrics().get(1).value()).isEqualByComparingTo("15");
        assertThat(summary.metrics().get(2).value()).isEqualByComparingTo("16");
    }

    @Test
    void emptyExistingUnitHasZeroValuesAndNeutralTrend() {
        var empty = snapshot(tree, List.of(), List.of());
        var summary = calculator.calculate(empty, empty, "A");
        summary.metrics().forEach(m -> {
            assertThat(m.value()).isZero();
            assertThat(m.previous()).isZero();
            assertThat(m.trend()).isEqualTo("neutral");
        });
        assertThat(summary.delta()).isEqualTo(new Summary.IssueDelta(0, 0, 0));
    }

    @Test
    void missingPreviousUnitHasNoComparisonInsteadOfZeroes() {
        var previous = snapshot(List.of(tree.getFirst()), List.of(), List.of());
        var current = snapshot(tree, List.of(), List.of());
        var summary = calculator.calculate(current, previous, "A");
        assertThat(summary.comparedTo()).isNotNull();
        assertThat(summary.delta()).isNull();
        summary.metrics().forEach(m -> assertThat(m.previous()).isNull());
    }

    @Test
    void movingGoalAndChangingHierarchyUseEachRunsOwnSubtree() {
        var issue = new Snapshot.IssueData("old-id", "GOAL_NO_OWNER", "G", Severity.CRITICAL);
        var previous = snapshot(tree, List.of(new Snapshot.GoalData("G", "A")), List.of(issue));
        var current = snapshot(tree, List.of(new Snapshot.GoalData("G", "B")), List.of(
                new Snapshot.IssueData("new-id", "GOAL_NO_OWNER", "G", Severity.CRITICAL)));
        assertThat(calculator.calculate(current, previous, "A").delta()).isEqualTo(new Summary.IssueDelta(0, 0, 1));
        assertThat(calculator.calculate(current, previous, "B").delta()).isEqualTo(new Summary.IssueDelta(1, 0, 0));
        assertThat(calculator.calculate(current, previous, "HQ").delta()).isEqualTo(new Summary.IssueDelta(0, 1, 0));
        var nested = snapshot(List.of(tree.get(0), tree.get(1), new Snapshot.UnitData("B", "B", "A")),
                current.goals(), current.issues());
        assertThat(calculator.calculate(nested, current, "A").delta()).isEqualTo(new Summary.IssueDelta(1, 0, 0));
    }

    @Test
    void deltaCalculatorSeparatesThreeCategoriesByRuleAndObject() {
        var common = new Snapshot.ProblemKey("RULE", "G");
        var current = Set.of(common, new Snapshot.ProblemKey("NEW", "G"));
        var previous = Set.of(common, new Snapshot.ProblemKey("OLD", "G"));
        assertThat(new IssueDeltaCalculator().calculate(current, previous))
                .isEqualTo(new Summary.IssueDelta(1, 1, 1));
    }

    @Test
    void rejectsUnknownUnitAndProtectsTraversalFromCycles() {
        assertThatThrownBy(() -> calculator.calculate(snapshot(tree, List.of(), List.of()), null, "missing"))
                .isInstanceOf(UnitNotFoundException.class);
        var cycle = snapshot(List.of(new Snapshot.UnitData("A", "A", "B"), new Snapshot.UnitData("B", "B", "A")),
                List.of(), List.of());
        assertThatThrownBy(() -> calculator.calculate(cycle, null, "A")).isInstanceOf(IllegalStateException.class);
    }

    private Snapshot snapshot(List<Snapshot.UnitData> units, List<Snapshot.GoalData> goals, List<Snapshot.IssueData> issues) {
        return new Snapshot("R", LocalDate.of(2026, 10, 26), null, units, goals, issues);
    }
}
