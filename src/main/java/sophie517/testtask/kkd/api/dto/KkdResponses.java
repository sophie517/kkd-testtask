package sophie517.testtask.kkd.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import sophie517.testtask.kkd.application.service.KkdQueryService;
import sophie517.testtask.kkd.application.summary.Summary;

public final class KkdResponses {
    private KkdResponses() { }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RunResponse(String id, String status, LocalDate snapshotDate,
                              String comparedToRunId, long issueCount) { }
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record RunReference(String id, LocalDate snapshotDate) { }
    public record UnitReference(String id, String name) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Metric(String key, String title, String unit, BigDecimal value,
                         BigDecimal previous, BigDecimal delta, String trend) { }
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SeverityCount(String code, long current, Long previous, Long delta) { }
    public record IssueDelta(@JsonProperty("new") int newCount, int persisting, int resolved) { }
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SummaryResponse(RunReference run, RunReference comparedTo, UnitReference unit,
                                  List<Metric> metrics, List<SeverityCount> issuesBySeverity, IssueDelta delta) { }
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record IssueResponse(String id, String runId, String ruleCode, String severity,
                                String goalId, LocalDate snapshotDate) { }
    public record IssuePage(List<IssueResponse> items, int total, int limit, int offset) { }
    public record ErrorDetail(String code, String message) { }
    public record ErrorResponse(ErrorDetail error) { }

    public static RunResponse from(KkdQueryService.RunItem item) {
        return new RunResponse(item.id(), item.status().name(), item.snapshotDate(),
                item.comparedToRunId(), item.issueCount());
    }

    public static SummaryResponse from(Summary summary) {
        var previous = summary.comparedTo();
        var delta = summary.delta();
        return new SummaryResponse(new RunReference(summary.run().id(), summary.run().snapshotDate()),
                previous == null ? null : new RunReference(previous.id(), previous.snapshotDate()),
                new UnitReference(summary.unit().id(), summary.unit().name()),
                summary.metrics().stream().map(m -> new Metric(m.key(), m.title(), m.unit(),
                        m.value(), m.previous(), m.delta(), m.trend())).toList(),
                summary.issuesBySeverity().stream().map(s -> new SeverityCount(
                        s.code().name().toLowerCase(Locale.ROOT), s.current(), s.previous(), s.delta())).toList(),
                delta == null ? null : new IssueDelta(delta.newCount(), delta.persisting(), delta.resolved()));
    }

    public static IssuePage from(KkdQueryService.IssuePage page) {
        return new IssuePage(page.items().stream().map(i -> new IssueResponse(i.id(), i.runId(),
                i.ruleCode(), i.severity().name().toLowerCase(Locale.ROOT), i.goalId(), i.snapshotDate())).toList(),
                page.total(), page.limit(), page.offset());
    }
}
