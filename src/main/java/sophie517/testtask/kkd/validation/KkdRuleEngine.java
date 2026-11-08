package sophie517.testtask.kkd.validation;

import java.util.Comparator;
import java.util.List;
import sophie517.testtask.kkd.domain.model.GoalStatus;
import sophie517.testtask.kkd.validation.impl.GoalWithoutKeyResults;
import sophie517.testtask.kkd.validation.impl.GoalWithoutOwner;
import sophie517.testtask.kkd.validation.impl.GoalWithoutParent;
import sophie517.testtask.kkd.validation.impl.KeyResultWeightsSum;

/** Pure Java check; creates neither persistence records nor run identifiers. */
public final class KkdRuleEngine {
    private final List<KkdRule> rules = List.of(new GoalWithoutOwner(),
            new GoalWithoutKeyResults(), new KeyResultWeightsSum(), new GoalWithoutParent());

    public List<Violation> execute(ValidationContext context) {
        return context.goals().stream().filter(goal -> goal.status() == GoalStatus.ACTIVE)
                .flatMap(goal -> rules.stream().filter(rule -> rule.isViolated(goal, context))
                        .map(rule -> new Violation(rule.code(), goal.id(), rule.severity())))
                .sorted(Comparator.comparing(Violation::ruleCode).thenComparing(Violation::goalId))
                .toList();
    }
}
