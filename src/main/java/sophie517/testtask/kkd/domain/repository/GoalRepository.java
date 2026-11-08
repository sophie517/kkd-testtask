package sophie517.testtask.kkd.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sophie517.testtask.kkd.domain.model.Goal;

public interface GoalRepository extends JpaRepository<Goal, String> {
}
