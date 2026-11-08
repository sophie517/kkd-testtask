package sophie517.testtask.kkd.validation;

import java.util.List;
import sophie517.testtask.kkd.validation.model.GoalData;
import sophie517.testtask.kkd.validation.model.KeyResultData;
import sophie517.testtask.kkd.validation.model.UnitData;

/** Prepared data for one check. Includes archived goals that may be parents. */
public record ValidationContext(List<UnitData> units, List<GoalData> goals,
                                List<KeyResultData> keyResults) {
    public ValidationContext {
        units = List.copyOf(units);
        goals = List.copyOf(goals);
        keyResults = List.copyOf(keyResults);
    }

    public UnitData unit(String id) {
        return units.stream().filter(unit -> unit.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown unit: " + id));
    }

    public GoalData goal(String id) {
        if (id == null) return null;
        return goals.stream().filter(goal -> goal.id().equals(id)).findFirst().orElse(null);
    }

    public List<KeyResultData> keyResultsFor(String goalId) {
        return keyResults.stream().filter(kr -> kr.goalId().equals(goalId)).toList();
    }
}
