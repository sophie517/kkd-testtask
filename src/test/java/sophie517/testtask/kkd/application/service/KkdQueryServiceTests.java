package sophie517.testtask.kkd.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import sophie517.testtask.kkd.domain.model.Run;
import sophie517.testtask.kkd.domain.model.RunStatus;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;

class KkdQueryServiceTests {
    @Test
    void sortsRunsByDateThenIdAndSupportsEmptyHistory() {
        var runs = mock(RunRepository.class);
        var issues = mock(IssueRepository.class);
        var service = new KkdQueryService(runs, issues, mock(SnapshotReader.class));
        var date = LocalDate.of(2026, 10, 26);
        when(runs.findAll()).thenReturn(List.of(new Run("Z", RunStatus.COMPLETED, date, null),
                new Run("old", RunStatus.COMPLETED, date.minusDays(7), null), new Run("A", RunStatus.COMPLETED, date, null)));
        assertThat(service.listRuns()).extracting(KkdQueryService.RunItem::id).containsExactly("A", "Z", "old");
        when(runs.findAll()).thenReturn(List.of());
        assertThat(service.listRuns()).isEmpty();
    }
}
