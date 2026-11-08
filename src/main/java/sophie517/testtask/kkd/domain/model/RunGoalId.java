package sophie517.testtask.kkd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RunGoalId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "run_id", nullable = false, length = 100)
    private String runId;

    @Column(name = "goal_id", nullable = false, length = 100)
    private String goalId;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RunGoalId that)) return false;
        return Objects.equals(runId, that.runId) && Objects.equals(goalId, that.goalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runId, goalId);
    }
}
