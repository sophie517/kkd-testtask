package sophie517.testtask.kkd.validation.impl;

import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.validation.KkdRule;
import sophie517.testtask.kkd.validation.ValidationContext;
import sophie517.testtask.kkd.validation.model.GoalData;

public final class GoalWithoutOwner implements KkdRule {
    public String code() { return "GOAL_NO_OWNER"; }

    public Severity severity() { return Severity.CRITICAL; }

    public boolean isViolated(GoalData goal, ValidationContext context) {
        return goal.ownerId() == null || goal.ownerId().isBlank();
    }
}
