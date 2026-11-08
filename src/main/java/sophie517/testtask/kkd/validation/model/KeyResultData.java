package sophie517.testtask.kkd.validation.model;

import java.math.BigDecimal;
import java.util.Objects;

public record KeyResultData(String goalId, BigDecimal weight) {
    public KeyResultData {
        Objects.requireNonNull(weight, "weight");
    }
}
