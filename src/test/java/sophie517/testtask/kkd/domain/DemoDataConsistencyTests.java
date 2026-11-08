package sophie517.testtask.kkd.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DemoDataConsistencyTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Set<String> RULES = Set.of(
            "GOAL_NO_OWNER", "GOAL_NO_KR", "KR_WEIGHTS_SUM", "CASCADE_NO_PARENT");

    @Test
    void currentDataMatchesSecondFixture() throws Exception {
        JsonNode current = fixture("run-002");
        assertThat(csv("unit")).containsExactlyElementsOf(rows(current.get("units")));
        assertThat(csv("goal")).containsExactlyElementsOf(rows(current.get("goals")));
        assertThat(csv("key_result")).containsExactlyElementsOf(rows(current.get("keyResults")));
        assertThat(current.get("goals").size()).isEqualTo(24);
        assertThat(current.get("units").size()).isEqualTo(3);
        assertThat(current.get("keyResults").size()).isEqualTo(40);
    }

    @Test
    void historyContainsEveryActiveGoalAndExactlyTheExpectedIssues() throws Exception {
        List<Map<String, String>> runs = csv("run");
        assertThat(runs).hasSize(2);
        for (String name : List.of("run-001", "run-002")) {
            JsonNode snapshot = fixture(name);
            String runId = snapshot.get("runId").asText();
            var run = runs.stream().filter(r -> r.get("id").equals(runId)).findFirst().orElseThrow();
            assertThat(run.get("snapshot_date")).isEqualTo(snapshot.get("snapshotDate").asText());
            assertThat(run.get("previous_run_id")).isEqualTo(value(snapshot.get("previousRunId")));
            assertThat(run.get("status")).isEqualTo("COMPLETED");

            var expectedGoals = rows(snapshot.get("goals")).stream()
                    .filter(g -> "ACTIVE".equals(g.get("status")))
                    .map(g -> Map.of("run_id", runId, "goal_id", g.get("id"), "unit_id", g.get("unit_id")))
                    .toList();
            assertThat(forRun(csv("run_goal"), runId)).containsExactlyInAnyOrderElementsOf(expectedGoals);
            assertThat(expectedGoals).hasSize(21);

            List<Map<String, String>> expectedUnits = new ArrayList<>();
            for (Map<String, String> unit : rows(snapshot.get("units"))) {
                Map<String, String> row = new HashMap<>(unit);
                row.put("unit_id", row.remove("id"));
                row.put("run_id", runId);
                expectedUnits.add(row);
            }
            assertThat(forRun(csv("run_unit"), runId)).containsExactlyInAnyOrderElementsOf(expectedUnits);
            var actualIssues = forRun(csv("issue"), runId).stream().map(issue -> Map.of(
                    "rule_code", issue.get("rule_code"), "goal_id", issue.get("goal_id"),
                    "severity", issue.get("severity"))).toList();
            assertThat(actualIssues).containsExactlyInAnyOrderElementsOf(rows(snapshot.get("expectedIssues")));
            assertThat(actualIssues.stream().map(i -> i.get("rule_code")).collect(Collectors.toSet()))
                    .isEqualTo(RULES);
        }
    }

    @Test
    void fixturesHaveAValidTreeAndAllThreeChangeCategories() throws Exception {
        for (String name : List.of("run-001", "run-002")) {
            var units = rows(fixture(name).get("units"));
            assertThat(units.stream().filter(u -> u.get("parent_unit_id") == null).count()).isEqualTo(1);
            Map<String, String> parents = new HashMap<>();
            units.forEach(u -> parents.put(u.get("id"), u.get("parent_unit_id")));
            for (String id : parents.keySet()) {
                Set<String> visited = new HashSet<>();
                for (String cursor = id; cursor != null; cursor = parents.get(cursor)) {
                    assertThat(parents).containsKey(cursor);
                    assertThat(visited.add(cursor)).isTrue();
                }
            }
        }
        Set<String> previous = keys(fixture("run-001"));
        Set<String> current = keys(fixture("run-002"));
        assertThat(previous).hasSize(9);
        assertThat(current).hasSize(8);
        assertThat(current.stream().filter(previous::contains).count()).isEqualTo(4);
        assertThat(current.stream().filter(k -> !previous.contains(k)).count()).isEqualTo(4);
        assertThat(previous.stream().filter(k -> !current.contains(k)).count()).isEqualTo(5);
        assertThat(csv("kkd_rule").stream().map(r -> r.get("code")).collect(Collectors.toSet())).isEqualTo(RULES);
    }

    private Set<String> keys(JsonNode fixture) {
        return rows(fixture.get("expectedIssues")).stream()
                .map(i -> i.get("rule_code") + ":" + i.get("goal_id")).collect(Collectors.toSet());
    }

    private JsonNode fixture(String name) throws Exception {
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/fixtures/" + name + ".json"))) {
            return mapper.readTree(stream);
        }
    }

    private List<Map<String, String>> csv(String table) throws Exception {
        String path = "/db/changelog/data/" + table + ".csv";
        try (var reader = new CSVReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(path)), StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            List<Map<String, String>> result = new ArrayList<>();
            for (String[] row; (row = reader.readNext()) != null;) {
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    values.put(headers[i], "NULL".equals(row[i]) ? null : row[i]);
                }
                result.add(values);
            }
            return result;
        }
    }

    private List<Map<String, String>> rows(JsonNode array) {
        List<Map<String, String>> result = new ArrayList<>();
        for (JsonNode row : array) {
            Map<String, String> values = new LinkedHashMap<>();
            row.properties().forEach(e -> values.put(
                    e.getKey().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT),
                    value(e.getValue())));
            result.add(values);
        }
        return result;
    }

    private String value(JsonNode node) {
        return node.isNull() ? null : node.asText();
    }

    private List<Map<String, String>> forRun(List<Map<String, String>> rows, String runId) {
        return rows.stream().filter(r -> runId.equals(r.get("run_id"))).toList();
    }
}
