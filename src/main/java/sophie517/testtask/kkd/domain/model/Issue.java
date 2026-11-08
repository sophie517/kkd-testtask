package sophie517.testtask.kkd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "issue")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "run_id", nullable = false, length = 100)
    private String runId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Column(name = "goal_id", nullable = false, length = 100)
    private String goalId;

    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

}
