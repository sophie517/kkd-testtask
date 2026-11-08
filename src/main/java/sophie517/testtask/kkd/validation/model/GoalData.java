package sophie517.testtask.kkd.validation.model;

import sophie517.testtask.kkd.domain.model.GoalStatus;

public record GoalData(String id, String ownerId, GoalStatus status,
                       String unitId, String parentGoalId) {
}
