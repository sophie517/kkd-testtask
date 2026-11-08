package sophie517.testtask.kkd.validation.impl;

import sophie517.testtask.kkd.domain.model.GoalStatus;
import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.validation.KkdRule;
import sophie517.testtask.kkd.validation.ValidationContext;
import sophie517.testtask.kkd.validation.model.GoalData;

public final class GoalWithoutParent implements KkdRule {
    public String code() { return "CASCADE_NO_PARENT"; }

    public Severity severity() { return Severity.MAJOR; }

    public boolean isViolated(GoalData goal, ValidationContext context) {
        String parentUnitId = context.unit(goal.unitId()).parentUnitId();
        if (parentUnitId == null) return false;
        GoalData parent = context.goal(goal.parentGoalId());
        return parent == null || parent.status() != GoalStatus.ACTIVE
                || !parentUnitId.equals(parent.unitId());
    }
}
