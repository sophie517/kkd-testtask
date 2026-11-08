package sophie517.testtask.kkd.application.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import sophie517.testtask.kkd.domain.model.Severity;

public record Summary(RunReference run, RunReference comparedTo, UnitReference unit,
                      List<Metric> metrics, List<SeverityCount> issuesBySeverity, IssueDelta delta) {
    public record RunReference(String id, LocalDate snapshotDate) { }
    public record UnitReference(String id, String name) { }
    public record Metric(String key, String title, String unit, BigDecimal value,
                         BigDecimal previous, BigDecimal delta, String trend) { }
    public record SeverityCount(Severity code, long current, Long previous, Long delta) { }
    public record IssueDelta(int newCount, int persisting, int resolved) { }
}
