package sophie517.testtask.kkd.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import sophie517.testtask.kkd.domain.model.GoalStatus;
import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.validation.impl.GoalWithoutKeyResults;
import sophie517.testtask.kkd.validation.impl.GoalWithoutOwner;
import sophie517.testtask.kkd.validation.impl.GoalWithoutParent;
import sophie517.testtask.kkd.validation.impl.KeyResultWeightsSum;
import sophie517.testtask.kkd.validation.model.GoalData;
import sophie517.testtask.kkd.validation.model.KeyResultData;
import sophie517.testtask.kkd.validation.model.UnitData;

class KkdRuleEngineTests {
    private final KkdRuleEngine engine = new KkdRuleEngine();
    private final GoalData goal = new GoalData("G", "EMP", GoalStatus.ACTIVE, "CHILD", "P");
    private final List<UnitData> units = List.of(new UnitData("HQ", null),
            new UnitData("CHILD", "HQ"), new UnitData("LEAF", "CHILD"));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void detectsMissingOwner(String owner) {
        var input = new GoalData("G", owner, GoalStatus.ACTIVE, "HQ", null);
        assertThat(new GoalWithoutOwner().isViolated(input, context(input))).isTrue();
    }

    @Test
    void acceptsOwnerAndAtLeastOneKeyResult() {
        var input = context(goal, "100");
        assertThat(new GoalWithoutOwner().isViolated(goal, input)).isFalse();
        assertThat(new GoalWithoutKeyResults().isViolated(goal, input)).isFalse();
    }

    @Test
    void missingKeyResultsSuppressesOnlyTheWeightRule() {
        var input = new GoalData("G", null, GoalStatus.ACTIVE, "CHILD", null);
        var result = engine.execute(context(input));
        assertThat(result).extracting(Violation::ruleCode)
                .containsExactly("CASCADE_NO_PARENT", "GOAL_NO_KR", "GOAL_NO_OWNER");
    }

    @ParameterizedTest
    @CsvSource({"99.99,true", "100.00,false", "100.01,true", "100,false", "100.000,false"})
    void comparesWeightsExactlyIgnoringScale(String weight, boolean expected) {
        assertThat(new KeyResultWeightsSum().isViolated(goal, context(goal, weight))).isEqualTo(expected);
    }

    @Test
    void addsFractionalWeightsWithoutRounding() {
        assertThat(new KeyResultWeightsSum().isViolated(goal,
                context(goal, "33.33", "33.33", "33.34"))).isFalse();
    }

    @Test
    void validatesImmediateActiveParentAndExemptsRoot() {
        var rule = new GoalWithoutParent();
        var root = new GoalData("G", "EMP", GoalStatus.ACTIVE, "HQ", null);
        assertThat(rule.isViolated(root, context(root))).isFalse();
        assertThat(rule.isViolated(goal, context(goal))).isTrue();
        for (var parent : List.of(
                new GoalData("P", "EMP", GoalStatus.ARCHIVED, "HQ", null),
                new GoalData("P", "EMP", GoalStatus.ACTIVE, "CHILD", null))) {
            assertThat(rule.isViolated(goal, new ValidationContext(units, List.of(goal, parent), List.of())))
                    .isTrue();
        }
        var validParent = new GoalData("P", "EMP", GoalStatus.ACTIVE, "HQ", null);
        assertThat(rule.isViolated(goal,
                new ValidationContext(units, List.of(goal, validParent), List.of()))).isFalse();
        var leaf = new GoalData("L", "EMP", GoalStatus.ACTIVE, "LEAF", "P");
        assertThat(rule.isViolated(leaf,
                new ValidationContext(units, List.of(leaf, validParent), List.of()))).isTrue();
    }

    @Test
    void doesNotCheckArchivedGoals() {
        var archived = new GoalData("G", null, GoalStatus.ARCHIVED, "CHILD", null);
        assertThat(engine.execute(context(archived))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"run-001", "run-002"})
    void matchesDemoIssuesAndIsIndependentOfInputOrder(String name) throws Exception {
        JsonNode data;
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/fixtures/" + name + ".json"))) {
            data = new ObjectMapper().readTree(stream);
        }
        List<UnitData> inputUnits = new ArrayList<>();
        data.get("units").forEach(u -> inputUnits.add(new UnitData(u.get("id").asText(), text(u, "parentUnitId"))));
        List<GoalData> goals = new ArrayList<>();
        data.get("goals").forEach(g -> goals.add(new GoalData(g.get("id").asText(), text(g, "ownerId"),
                GoalStatus.valueOf(g.get("status").asText()), g.get("unitId").asText(), text(g, "parentGoalId"))));
        List<KeyResultData> results = new ArrayList<>();
        data.get("keyResults").forEach(k -> results.add(new KeyResultData(
                k.get("goalId").asText(), new BigDecimal(k.get("weight").asText()))));
        List<Violation> expected = new ArrayList<>();
        data.get("expectedIssues").forEach(i -> expected.add(new Violation(i.get("ruleCode").asText(),
                i.get("goalId").asText(), Severity.valueOf(i.get("severity").asText().toUpperCase(Locale.ROOT)))));
        var context = new ValidationContext(inputUnits, goals, results);
        assertThat(engine.execute(context)).containsExactlyElementsOf(expected);
        Collections.reverse(goals);
        Collections.reverse(inputUnits);
        Collections.reverse(results);
        assertThat(engine.execute(new ValidationContext(inputUnits, goals, results)))
                .containsExactlyElementsOf(expected);
        assertThat(engine.execute(context)).containsExactlyElementsOf(expected);
    }

    private ValidationContext context(GoalData input, String... weights) {
        List<KeyResultData> results = new ArrayList<>();
        for (String weight : weights) results.add(new KeyResultData(input.id(), new BigDecimal(weight)));
        return new ValidationContext(units, List.of(input), results);
    }

    private String text(JsonNode node, String field) {
        return node.get(field).isNull() ? null : node.get(field).asText();
    }
}
