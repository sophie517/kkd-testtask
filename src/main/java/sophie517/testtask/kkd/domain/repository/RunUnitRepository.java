package sophie517.testtask.kkd.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import sophie517.testtask.kkd.domain.model.RunUnit;
import sophie517.testtask.kkd.domain.model.RunUnitId;

public interface RunUnitRepository extends JpaRepository<RunUnit, RunUnitId> {
    List<RunUnit> findAllByIdRunId(String runId);
}
