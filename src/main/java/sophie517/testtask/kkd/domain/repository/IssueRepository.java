package sophie517.testtask.kkd.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import sophie517.testtask.kkd.domain.model.Issue;

public interface IssueRepository extends JpaRepository<Issue, String> {
    List<Issue> findAllByRunId(String runId);
}
