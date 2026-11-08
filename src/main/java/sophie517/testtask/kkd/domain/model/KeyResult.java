package sophie517.testtask.kkd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "key_result")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeyResult {

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "goal_id", nullable = false, length = 100)
    private String goalId;

    @Column(name = "target_value", precision = 19, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "actual_value", precision = 19, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "actual_update_date")
    private LocalDate actualUpdateDate;

}
