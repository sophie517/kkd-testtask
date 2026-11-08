package sophie517.testtask.kkd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "goal")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Goal {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "owner_id", length = 100)
    private String ownerId;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private GoalStatus status;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @Column(name = "unit_id", nullable = false, length = 100)
    private String unitId;

    @Column(name = "parent_goal_id", length = 100)
    private String parentGoalId;

}
