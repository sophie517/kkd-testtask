package sophie517.testtask.kkd.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import liquibase.Scope;
import liquibase.changelog.ChangeLogParameters;
import liquibase.database.core.PostgresDatabase;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import org.junit.jupiter.api.Test;

class MigrationPlanTests {
    @Test
    void allMigrationsParseGeneratePostgresSqlAndDeclareRollback() throws Exception {
        try (var resources = new ClassLoaderResourceAccessor()) {
            Scope.child(Scope.Attr.resourceAccessor, resources, () -> {
                var database = new PostgresDatabase();
                String path = "db/changelog/changelog.yml";
                var changelog = ChangeLogParserFactory.getInstance().getParser(path, resources)
                        .parse(path, new ChangeLogParameters(), resources);
                assertThat(changelog.getChangeSets()).hasSize(4);
                List<String> sql = new ArrayList<>();
                for (var changeSet : changelog.getChangeSets()) {
                    assertThat(changeSet.supportsRollback(database)).as(changeSet.getId()).isTrue();
                    assertThat(changeSet.getRollback().getChanges()).isNotEmpty();
                    for (var change : changeSet.getChanges()) {
                        for (var statement : change.generateStatements(database)) {
                            for (var generated : SqlGeneratorFactory.getInstance().generateSql(statement, database)) {
                                sql.add(generated.toSql());
                            }
                        }
                    }
                }
                assertThat(sql.stream().filter(s -> s.startsWith("CREATE TABLE ")).count()).isEqualTo(8);
                assertThat(String.join("\n", sql)).contains("ON DELETE CASCADE", "PRIMARY KEY", "UNIQUE")
                        .doesNotContain("'NULL'");
            });
        }
    }
}
