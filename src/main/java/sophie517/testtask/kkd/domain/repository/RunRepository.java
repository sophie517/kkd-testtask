package sophie517.testtask.kkd.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sophie517.testtask.kkd.domain.model.Run;

public interface RunRepository extends JpaRepository<Run, String> {
}
