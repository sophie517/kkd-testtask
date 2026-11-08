package sophie517.testtask.kkd.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Test;
import sophie517.testtask.kkd.domain.model.Goal;
import sophie517.testtask.kkd.domain.model.Issue;
import sophie517.testtask.kkd.domain.model.KeyResult;
import sophie517.testtask.kkd.domain.model.KkdRule;
import sophie517.testtask.kkd.domain.model.Run;
import sophie517.testtask.kkd.domain.model.RunGoal;
import sophie517.testtask.kkd.domain.model.RunUnit;
import sophie517.testtask.kkd.domain.model.SeverityConverter;
import sophie517.testtask.kkd.domain.model.Unit;

class JpaMappingTests {
    @Test
    void entityMappingsAndCompositeIdentifiersAreValidWithoutJdbcAccess() {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .build();
        try {
            var sources = new MetadataSources(registry);
            for (var type : new Class<?>[] {Unit.class, Goal.class, KeyResult.class, KkdRule.class,
                    Run.class, RunUnit.class, RunGoal.class, Issue.class}) {
                sources.addAnnotatedClass(type);
            }
            var metadata = sources.getMetadataBuilder()
                    .applyAttributeConverter(SeverityConverter.class).build();
            ((MetadataImplementor) metadata).validate();
            assertThat(metadata.getEntityBindings()).hasSize(8);
            metadata.getEntityBindings().forEach(binding -> assertThat(binding.getIdentifier()).isNotNull());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
