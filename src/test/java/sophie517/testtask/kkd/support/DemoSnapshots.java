package sophie517.testtask.kkd.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import sophie517.testtask.kkd.application.summary.Snapshot;
import sophie517.testtask.kkd.domain.model.Severity;

public final class DemoSnapshots {
    private DemoSnapshots() { }

    public static Snapshot load(String name) throws Exception {
        try (var stream = Objects.requireNonNull(DemoSnapshots.class.getResourceAsStream("/fixtures/" + name + ".json"))) {
            var data = new ObjectMapper().readTree(stream);
            var units = new ArrayList<Snapshot.UnitData>();
            var goals = new ArrayList<Snapshot.GoalData>();
            var issues = new ArrayList<Snapshot.IssueData>();
            data.get("units").forEach(u -> units.add(new Snapshot.UnitData(u.get("id").asText(),
                    u.get("name").asText(), u.get("parentUnitId").isNull() ? null : u.get("parentUnitId").asText())));
            data.get("goals").forEach(g -> {
                if (g.get("status").asText().equals("ACTIVE")) {
                    goals.add(new Snapshot.GoalData(g.get("id").asText(), g.get("unitId").asText()));
                }
            });
            data.get("expectedIssues").forEach(i -> issues.add(new Snapshot.IssueData(name + "-" + issues.size(),
                    i.get("ruleCode").asText(), i.get("goalId").asText(),
                    Severity.valueOf(i.get("severity").asText().toUpperCase(Locale.ROOT)))));
            return new Snapshot(data.get("runId").asText(), LocalDate.parse(data.get("snapshotDate").asText()),
                    data.get("previousRunId").isNull() ? null : data.get("previousRunId").asText(), units, goals, issues);
        }
    }
}
