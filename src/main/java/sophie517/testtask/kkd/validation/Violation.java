package sophie517.testtask.kkd.validation;

import sophie517.testtask.kkd.domain.model.Severity;

public record Violation(String ruleCode, String goalId, Severity severity) {
}
