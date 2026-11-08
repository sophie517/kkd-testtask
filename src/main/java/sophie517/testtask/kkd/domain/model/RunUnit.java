package sophie517.testtask.kkd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "run_unit")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunUnit {

    @EmbeddedId
    private RunUnitId id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "parent_unit_id", length = 100)
    private String parentUnitId;

}
