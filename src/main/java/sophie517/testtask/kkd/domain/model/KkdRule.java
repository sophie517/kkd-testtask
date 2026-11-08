package sophie517.testtask.kkd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kkd_rule")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KkdRule {

    @Id
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

}
