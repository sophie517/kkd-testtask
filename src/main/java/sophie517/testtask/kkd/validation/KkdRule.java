package sophie517.testtask.kkd.validation;

import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.validation.model.GoalData;

/** A rule receives an active goal; the engine performs the status filtering. */
public interface KkdRule {
    String code();

    Severity severity();

    boolean isViolated(GoalData goal, ValidationContext context);
}
