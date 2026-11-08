package sophie517.testtask.kkd.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sophie517.testtask.kkd.domain.model.Goal;
import sophie517.testtask.kkd.domain.model.GoalStatus;
import sophie517.testtask.kkd.domain.model.Issue;
import sophie517.testtask.kkd.domain.model.KeyResult;
import sophie517.testtask.kkd.domain.model.Run;
import sophie517.testtask.kkd.domain.model.RunGoal;
import sophie517.testtask.kkd.domain.model.RunStatus;
import sophie517.testtask.kkd.domain.model.RunUnit;
import sophie517.testtask.kkd.domain.model.Unit;
import sophie517.testtask.kkd.domain.repository.GoalRepository;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.KeyResultRepository;
import sophie517.testtask.kkd.domain.repository.RunGoalRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;
import sophie517.testtask.kkd.domain.repository.RunUnitRepository;
import sophie517.testtask.kkd.domain.repository.UnitRepository;

class RunServiceTests {
    private final UnitRepository units = mock(UnitRepository.class);
    private final GoalRepository goals = mock(GoalRepository.class);
    private final KeyResultRepository keyResults = mock(KeyResultRepository.class);
    private final RunRepository runs = mock(RunRepository.class);
    private final RunUnitRepository runUnits = mock(RunUnitRepository.class);
    private final RunGoalRepository runGoals = mock(RunGoalRepository.class);
    private final IssueRepository issues = mock(IssueRepository.class);
    private final RunService service = new RunService(units, goals, keyResults, runs, runUnits, runGoals, issues);
    private final List<Run> savedRuns = new ArrayList<>();
    private final List<RunUnit> savedUnits = new ArrayList<>();
    private final List<RunGoal> savedGoals = new ArrayList<>();
    private final List<Issue> savedIssues = new ArrayList<>();
    private final LocalDate date = LocalDate.of(2026, 10, 26);

    @BeforeEach
    void captureWrites() {
        when(units.findAll()).thenReturn(List.of(new Unit("HQ", "Root", null)));
        when(goals.findAll()).thenReturn(List.of());
        when(keyResults.findAll()).thenReturn(List.of());
        doAnswer(call -> { Run value = call.getArgument(0); savedRuns.add(value); return value; })
                .when(runs).saveAndFlush(any(Run.class));
        doAnswer(call -> { RunUnit value = call.getArgument(0); savedUnits.add(value); return value; })
                .when(runUnits).saveAndFlush(any(RunUnit.class));
        doAnswer(call -> { List<RunGoal> values = call.getArgument(0); savedGoals.addAll(values); return values; })
                .when(runGoals).saveAllAndFlush(anyList());
        doAnswer(call -> { List<Issue> values = call.getArgument(0); savedIssues.addAll(values); return values; })
                .when(issues).saveAllAndFlush(anyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"run-001", "run-002"})
    void savesFullSnapshotAndExactFixtureViolations(String name) throws Exception {
        JsonNode fixture;
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/fixtures/" + name + ".json"))) {
            fixture = new ObjectMapper().readTree(stream);
        }
        List<Unit> inputUnits = new ArrayList<>();
        fixture.get("units").forEach(u -> inputUnits.add(new Unit(u.get("id").asText(),
                u.get("name").asText(), text(u, "parentUnitId"))));
        List<Goal> inputGoals = new ArrayList<>();
        fixture.get("goals").forEach(g -> inputGoals.add(new Goal(g.get("id").asText(), text(g, "ownerId"),
                GoalStatus.valueOf(g.get("status").asText()), LocalDate.parse(g.get("deadline").asText()),
                g.get("unitId").asText(), text(g, "parentGoalId"))));
        List<KeyResult> inputResults = new ArrayList<>();
        fixture.get("keyResults").forEach(k -> inputResults.add(new KeyResult(k.get("id").asText(),
                k.get("goalId").asText(), decimal(k, "targetValue"), decimal(k, "actualValue"),
                decimal(k, "weight"), LocalDate.parse(k.get("actualUpdateDate").asText()))));
        when(units.findAll()).thenReturn(inputUnits.reversed());
        when(goals.findAll()).thenReturn(inputGoals);
        when(keyResults.findAll()).thenReturn(inputResults);
        String previous = text(fixture, "previousRunId");
        if (previous != null) {
            when(runs.findById(previous)).thenReturn(Optional.of(new Run(previous, RunStatus.COMPLETED,
                    LocalDate.of(2026, 10, 19), null)));
        }

        var result = service.start(LocalDate.parse(fixture.get("snapshotDate").asText()), previous);
        assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.previousRunId()).isEqualTo(previous);
        assertThat(result.issueCount()).isEqualTo(fixture.get("expectedIssues").size());
        assertThat(savedRuns).hasSize(1);
        assertThat(savedRuns.getFirst().getSnapshotDate()).isEqualTo(result.snapshotDate());
        assertThat(savedUnits).extracting(u -> u.getId().getUnitId()).containsExactly("HQ", "UNIT-A", "UNIT-B");
        assertThat(savedGoals).extracting(g -> g.getId().getGoalId())
                .containsExactlyInAnyOrderElementsOf(inputGoals.stream().filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                        .map(Goal::getId).toList());
        assertThat(savedGoals).hasSize(21);
        List<String> expected = new ArrayList<>();
        fixture.get("expectedIssues").forEach(i -> expected.add(i.get("ruleCode").asText() + ":"
                + i.get("goalId").asText() + ":" + i.get("severity").asText()));
        assertThat(savedIssues).extracting(i -> i.getRuleCode() + ":" + i.getGoalId() + ":"
                + i.getSeverity().name().toLowerCase(java.util.Locale.ROOT))
                .containsExactlyInAnyOrderElementsOf(expected);
        UUID.fromString(result.id());
        savedIssues.forEach(i -> {
            UUID.fromString(i.getId());
            assertThat(i.getRunId()).isEqualTo(result.id());
        });
    }

    @Test
    void rejectsMalformedTreesBeforeAnyWrite() {
        for (var tree : List.of(
                List.<Unit>of(),
                List.of(new Unit("HQ", "Root", null), new Unit("OTHER", "Other", null)),
                List.of(new Unit("HQ", "Root", null), new Unit("A", "A", "MISSING")),
                List.of(new Unit("HQ", "Root", null), new Unit("A", "A", "B"), new Unit("B", "B", "A")))) {
            when(units.findAll()).thenReturn(tree);
            assertThatThrownBy(() -> service.start(date, null)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(runs, runUnits, runGoals, issues);
    }

    @Test
    void rejectsUnknownPreviousRunWithoutReadingOrSavingSnapshot() {
        assertThatThrownBy(() -> service.start(date, "missing")).isInstanceOf(RunNotFoundException.class);
        verifyNoInteractions(units, goals, keyResults, runUnits, runGoals, issues);
        assertThat(savedRuns).isEmpty();
    }

    @Test
    void propagatesWriteFailureToTheTransactionBoundary() {
        when(goals.findAll()).thenReturn(List.of(new Goal("G", null, GoalStatus.ACTIVE, date, "HQ", null)));
        doThrow(new IllegalStateException("write failed")).when(issues).saveAllAndFlush(anyList());
        assertThatThrownBy(() -> service.start(date, null)).isInstanceOf(IllegalStateException.class)
                .hasMessage("write failed");
        // This verifies exception propagation, not a database rollback.
    }

    @Test
    void newRunDoesNotModifyPreviouslyCreatedSnapshot() {
        when(goals.findAll()).thenReturn(List.of(new Goal("G", null, GoalStatus.ACTIVE, date, "HQ", null)));
        var first = service.start(date, null);
        RunUnit originalUnit = savedUnits.getFirst();
        RunGoal originalGoal = savedGoals.getFirst();
        List<Issue> originalIssues = List.copyOf(savedIssues);
        when(runs.findById(first.id())).thenReturn(Optional.of(savedRuns.getFirst()));
        when(units.findAll()).thenReturn(List.of(new Unit("HQ", "Renamed", null)));
        when(goals.findAll()).thenReturn(List.of(new Goal("G", "EMP", GoalStatus.ARCHIVED, date, "HQ", null)));

        var second = service.start(date.plusDays(7), first.id());
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.previousRunId()).isEqualTo(first.id());
        assertThat(second.issueCount()).isZero();
        assertThat(savedGoals).hasSize(1);
        assertThat(originalGoal.getId().getRunId()).isEqualTo(first.id());
        assertThat(originalUnit.getName()).isEqualTo("Root");
        assertThat(savedUnits.getLast().getName()).isEqualTo("Renamed");
        assertThat(savedIssues).containsExactlyElementsOf(originalIssues);
    }

    private String text(JsonNode node, String field) {
        return node.get(field).isNull() ? null : node.get(field).asText();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? null : new BigDecimal(value);
    }
}
