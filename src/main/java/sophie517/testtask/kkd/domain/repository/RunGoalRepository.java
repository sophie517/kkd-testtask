package sophie517.testtask.kkd.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import sophie517.testtask.kkd.domain.model.RunGoal;
import sophie517.testtask.kkd.domain.model.RunGoalId;

public interface RunGoalRepository extends JpaRepository<RunGoal, RunGoalId> {
    List<RunGoal> findAllByIdRunId(String runId);
}
