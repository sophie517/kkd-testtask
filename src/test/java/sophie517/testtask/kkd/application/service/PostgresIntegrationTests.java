package sophie517.testtask.kkd.application.service;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Run only against a disposable PostgreSQL database seeded by Liquibase. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "KKD_INTEGRATION_TESTS", matches = "true")
class PostgresIntegrationTests {
    @Autowired RunService runs;
    @Autowired KkdSummaryService summaries;
    @Autowired JdbcTemplate jdbc;

    private List<Long> counts() {
        return List.of("run", "run_unit", "run_goal", "issue").stream()
                .map(table -> jdbc.queryForObject("select count(*) from " + table, Long.class)).toList();
    }

    @Test void postgresRejectsInvalidDataAndCascadesCurrentKeyResults() {
        for (String sql : List.of(
                "update goal set status='INVALID' where id='GOAL-001'",
                "update goal set deadline=null where id='GOAL-001'",
                "update goal set unit_id='MISSING' where id='GOAL-001'",
                "update unit set parent_unit_id=id where id='UNIT-A'",
                "update key_result set weight=-1 where id='KR-001-1'",
                "update issue set severity='invalid' where id='ISSUE-02-001'",
                "update run set previous_run_id=id where id='RUN-002'",
                "insert into issue select 'INTEGRATION-DUP',run_id,rule_code,goal_id,severity from issue where id='ISSUE-02-001'",
                "update issue set goal_id='GOAL-008' where id='ISSUE-02-001'",
                "update run_goal set unit_id='MISSING' where run_id='RUN-002' and goal_id='GOAL-001'",
                "delete from run where id='RUN-001'")) {
            assertThrows(DataAccessException.class, () -> jdbc.execute(sql), sql);
        }
        jdbc.execute("insert into goal select 'INTEGRATION-GOAL',owner_id,status,deadline,unit_id,parent_goal_id from goal where id='GOAL-001'");
        try {
            jdbc.execute("insert into key_result select 'INTEGRATION-KR','INTEGRATION-GOAL',target_value,actual_value,weight,actual_update_date from key_result where id='KR-001-1'");
        } finally {
            jdbc.execute("delete from goal where id='INTEGRATION-GOAL'");
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from key_result where id='INTEGRATION-KR'", Integer.class));
    }

    @Test void databaseFailureRollsBackAllFlushedRows() {
        var before = counts();
        jdbc.execute("alter table issue add constraint integration_reject_run check (run_id in ('RUN-001','RUN-002'))");
        try {
            assertThrows(DataAccessException.class, () -> runs.start(LocalDate.of(2026, 11, 8), "RUN-002"));
            assertEquals(before, counts());
        } finally {
            jdbc.execute("alter table issue drop constraint integration_reject_run");
        }
    }

    @Test void realRunUsesRepeatableReadAndHistorySurvivesSourceChanges() {
        var before = summaries.getSummary("RUN-002", "HQ");
        jdbc.execute("create function integration_isolation() returns trigger language plpgsql as $$ begin if current_setting('transaction_isolation') <> 'repeatable read' then raise exception 'incorrect isolation'; end if; return NEW; end $$");
        jdbc.execute("create trigger integration_isolation before insert on issue for each row execute function integration_isolation()");
        String created = null;
        try {
            var result = runs.start(LocalDate.of(2026, 11, 8), "RUN-002");
            created = result.id();
            assertEquals(8, result.issueCount());
            assertEquals(21, jdbc.queryForObject("select count(*) from run_goal where run_id=?", Integer.class, created));
            assertEquals(3, jdbc.queryForObject("select count(*) from run_unit where run_id=?", Integer.class, created));
            jdbc.update("update unit set name='Integration changed' where id='HQ'");
            jdbc.update("update goal set status='ARCHIVED' where id='GOAL-003'");
            assertEquals(before, summaries.getSummary("RUN-002", "HQ"));
        } finally {
            jdbc.execute("drop trigger integration_isolation on issue");
            jdbc.execute("drop function integration_isolation()");
            jdbc.update("update unit set name=(select name from run_unit where run_id='RUN-002' and unit_id='HQ') where id='HQ'");
            jdbc.update("update goal set status='ACTIVE' where id='GOAL-003'");
            if (created != null) {
                jdbc.update("delete from run where id=?", created);
                assertEquals(0, jdbc.queryForObject("select count(*) from run_unit where run_id=?", Integer.class, created));
                assertEquals(0, jdbc.queryForObject("select count(*) from run_goal where run_id=?", Integer.class, created));
                assertEquals(0, jdbc.queryForObject("select count(*) from issue where run_id=?", Integer.class, created));
            }
        }
    }
}
