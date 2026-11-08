package sophie517.testtask.kkd.domain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.changelog.FastCheckService;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL migration cycle, isolated in a new schema and enabled explicitly. */
@EnabledIfEnvironmentVariable(named = "KKD_MIGRATION_TESTS", matches = "true")
class MigrationCycleTests {
    private static final List<String> TABLES = List.of(
            "goal", "issue", "key_result", "kkd_rule", "run", "run_goal", "run_unit", "unit");

    @Test
    void updateRollbackAndUpdateRestoreSchemaAndEveryDemoRow() throws Exception {
        String url = required("MIGRATION_DB_URL");
        String username = required("MIGRATION_DB_USERNAME");
        String password = required("MIGRATION_DB_PASSWORD");
        String schema = "migration_check_" + UUID.randomUUID().toString().replace("-", "");

        try (var admin = DriverManager.getConnection(url, username, password)) {
            execute(admin, "CREATE SCHEMA " + schema);
            admin.setSchema(schema);
            try {
                try (var resources = new ClassLoaderResourceAccessor()) {
                    var connection = admin;
                    var contexts = new Contexts();
                    var labels = new LabelExpression();

                    migrate(url, username, password, schema, resources, lb -> lb.update(contexts, labels));
                    assertEquals(TABLES, rows(connection, "SELECT tablename FROM pg_tables "
                            + "WHERE schemaname=current_schema() AND tablename NOT IN "
                            + "('databasechangelog','databasechangeloglock') ORDER BY tablename"));
                    assertEquals(List.of("4"), rows(connection, "SELECT count(*) FROM databasechangelog"));
                    assertEquals(List.of("3", "24", "40", "4", "2", "6", "42", "17"),
                            List.of("unit", "goal", "key_result", "kkd_rule", "run", "run_unit", "run_goal", "issue")
                                    .stream().map(table -> count(connection, table)).toList());
                    var before = snapshot(connection);

                    migrate(url, username, password, schema, resources, lb -> lb.update(contexts, labels));
                    assertEquals(before, snapshot(connection), "Repeated update must not change schema or seed");
                    assertEquals(List.of("4"), rows(connection, "SELECT count(*) FROM databasechangelog"));

                    migrate(url, username, password, schema, resources, lb -> lb.rollback(4, contexts, labels));
                    assertEquals(List.of("databasechangelog", "databasechangeloglock"), rows(connection,
                            "SELECT tablename FROM pg_tables WHERE schemaname=current_schema() ORDER BY tablename"));
                    assertEquals(List.of("0"), rows(connection, "SELECT count(*) FROM databasechangelog"));

                    migrate(url, username, password, schema, resources, lb -> lb.update(contexts, labels));
                    assertEquals(before, snapshot(connection), "Reapplied schema and every demo row must match");
                    assertEquals(List.of("4"), rows(connection, "SELECT count(*) FROM databasechangelog"));
                }
            } finally {
                // Only this test's uniquely generated schema is removed, including on assertion failure.
                execute(admin, "DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    @FunctionalInterface
    private interface MigrationOperation {
        void run(Liquibase liquibase) throws Exception;
    }

    private static void migrate(String url, String username, String password, String schema,
                                ClassLoaderResourceAccessor resources, MigrationOperation operation) throws Exception {
        // Each invocation has a fresh changelog cache, like separate CLI processes.
        Scope.getCurrentScope().getSingleton(FastCheckService.class).clearCache();
        try (var connection = DriverManager.getConnection(url, username, password)) {
            connection.setSchema(schema);
            var database = new PostgresDatabase();
            database.setConnection(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            database.setLiquibaseSchemaName(schema);
            try (var liquibase = new Liquibase("db/changelog/changelog.yml", resources, database)) {
                operation.run(liquibase);
            }
        }
    }

    private static Map<String, List<String>> snapshot(Connection connection) throws Exception {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String table : TABLES) {
            result.put(table, rows(connection,
                    "SELECT row_to_json(t)::text FROM " + table + " t ORDER BY row_to_json(t)::text"));
        }
        result.put("columns", rows(connection, """
                SELECT row_to_json(c)::text FROM (
                    SELECT table_name, column_name, ordinal_position, data_type, udt_name,
                           is_nullable, column_default, character_maximum_length, numeric_precision, numeric_scale
                    FROM information_schema.columns
                    WHERE table_schema=current_schema()
                      AND table_name NOT IN ('databasechangelog','databasechangeloglock')
                    ORDER BY table_name, ordinal_position
                ) c
                """));
        result.put("constraints", rows(connection, """
                SELECT r.relname || ':' || c.conname || ':' || pg_get_constraintdef(c.oid)
                FROM pg_constraint c JOIN pg_class r ON r.oid=c.conrelid
                JOIN pg_namespace n ON n.oid=r.relnamespace
                WHERE n.nspname=current_schema()
                  AND r.relname NOT IN ('databasechangelog','databasechangeloglock')
                ORDER BY r.relname, c.conname
                """));
        result.put("indexes", rows(connection, """
                SELECT indexdef FROM pg_indexes WHERE schemaname=current_schema()
                  AND tablename NOT IN ('databasechangelog','databasechangeloglock')
                ORDER BY tablename, indexname
                """));
        return result;
    }

    private static List<String> rows(Connection connection, String sql) throws Exception {
        List<String> result = new ArrayList<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            while (rows.next()) result.add(rows.getString(1));
        }
        return result;
    }

    private static String count(Connection connection, String table) {
        try {
            return rows(connection, "SELECT count(*) FROM " + table).getFirst();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
        return value;
    }
}
