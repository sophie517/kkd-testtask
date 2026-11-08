package sophie517.testtask.kkd.application.summary;

import java.util.Set;

public final class IssueDeltaCalculator {
    public Summary.IssueDelta calculate(Set<Snapshot.ProblemKey> current, Set<Snapshot.ProblemKey> previous) {
        int persisting = (int) current.stream().filter(previous::contains).count();
        return new Summary.IssueDelta(current.size() - persisting, persisting, previous.size() - persisting);
    }
}
