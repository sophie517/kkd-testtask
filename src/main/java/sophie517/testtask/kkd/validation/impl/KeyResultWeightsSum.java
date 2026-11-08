package sophie517.testtask.kkd.validation.impl;

import java.math.BigDecimal;
import sophie517.testtask.kkd.domain.model.Severity;
import sophie517.testtask.kkd.validation.KkdRule;
import sophie517.testtask.kkd.validation.ValidationContext;
import sophie517.testtask.kkd.validation.model.GoalData;
import sophie517.testtask.kkd.validation.model.KeyResultData;

public final class KeyResultWeightsSum implements KkdRule {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public String code() { return "KR_WEIGHTS_SUM"; }

    public Severity severity() { return Severity.MAJOR; }

    public boolean isViolated(GoalData goal, ValidationContext context) {
        var results = context.keyResultsFor(goal.id());
        if (results.isEmpty()) return false;
        BigDecimal sum = results.stream().map(KeyResultData::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.compareTo(HUNDRED) != 0;
    }
}
