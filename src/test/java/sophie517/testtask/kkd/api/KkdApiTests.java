package sophie517.testtask.kkd.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sophie517.testtask.kkd.api.controller.KkdController;
import sophie517.testtask.kkd.api.exception.ApiExceptionHandler;
import sophie517.testtask.kkd.application.service.KkdQueryService;
import sophie517.testtask.kkd.application.service.KkdSummaryService;
import sophie517.testtask.kkd.application.service.SnapshotReader;
import sophie517.testtask.kkd.domain.model.Issue;
import sophie517.testtask.kkd.domain.model.Run;
import sophie517.testtask.kkd.domain.model.RunGoal;
import sophie517.testtask.kkd.domain.model.RunGoalId;
import sophie517.testtask.kkd.domain.model.RunStatus;
import sophie517.testtask.kkd.domain.model.RunUnit;
import sophie517.testtask.kkd.domain.model.RunUnitId;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.RunGoalRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;
import sophie517.testtask.kkd.domain.repository.RunUnitRepository;
import sophie517.testtask.kkd.support.DemoSnapshots;

class KkdApiTests {
    private final RunRepository runs = mock(RunRepository.class);
    private final RunUnitRepository units = mock(RunUnitRepository.class);
    private final RunGoalRepository goals = mock(RunGoalRepository.class);
    private final IssueRepository issues = mock(IssueRepository.class);
    private MockMvc mvc;

    @BeforeEach
    void prepareHistoricalDataWithoutDatabaseOrServer() throws Exception {
        List<Run> savedRuns = new ArrayList<>();
        List<Issue> savedIssues = new ArrayList<>();
        for (String name : List.of("run-001", "run-002")) {
            var snapshot = DemoSnapshots.load(name);
            Run run = new Run(snapshot.id(), RunStatus.COMPLETED, snapshot.date(), snapshot.previousRunId());
            savedRuns.add(run);
            when(runs.findById(run.getId())).thenReturn(Optional.of(run));
            when(units.findAllByIdRunId(run.getId())).thenReturn(snapshot.units().stream()
                    .map(u -> new RunUnit(new RunUnitId(run.getId(), u.id()), u.name(), u.parentUnitId())).toList());
            when(goals.findAllByIdRunId(run.getId())).thenReturn(snapshot.goals().stream()
                    .map(g -> new RunGoal(new RunGoalId(run.getId(), g.id()), g.unitId())).toList());
            var problemRows = snapshot.issues().stream().map(i -> new Issue(i.id(), snapshot.id(),
                    i.ruleCode(), i.goalId(), i.severity())).toList();
            when(issues.findAllByRunId(run.getId())).thenReturn(problemRows);
            savedIssues.addAll(problemRows);
        }
        when(runs.findAll()).thenReturn(savedRuns);
        when(issues.findAll()).thenReturn(savedIssues);
        var reader = new SnapshotReader(runs, units, goals, issues);
        var controller = new KkdController(new KkdSummaryService(reader), new KkdQueryService(runs, issues, reader));
        ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
    }

    @Test
    void returnsRunMetadataAndHistoricalSummary() throws Exception {
        mvc.perform(get("/api/runs")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("RUN-002"))
                .andExpect(jsonPath("$[0].snapshot_date").value("2026-10-26"))
                .andExpect(jsonPath("$[0].compared_to_run_id").value("RUN-001"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].issue_count").value(8));
        mvc.perform(get("/api/kkd/summary?run_id=RUN-002&unit_id=HQ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit.name").value("Организация"))
                .andExpect(jsonPath("$.metrics[0].value").value(66.7))
                .andExpect(jsonPath("$.metrics[0].previous").value(61.9))
                .andExpect(jsonPath("$.metrics[0].delta").value(4.8))
                .andExpect(jsonPath("$.metrics[0].trend").value("better"))
                .andExpect(jsonPath("$.metrics[1].value").value(7))
                .andExpect(jsonPath("$.metrics[2].value").value(8))
                .andExpect(jsonPath("$.issues_by_severity[2].code").value("minor"))
                .andExpect(jsonPath("$.issues_by_severity[2].current").value(0))
                .andExpect(jsonPath("$.delta.new").value(4))
                .andExpect(jsonPath("$.delta.persisting").value(4))
                .andExpect(jsonPath("$.delta.resolved").value(5));
    }

    @Test
    void firstRunSerializesNullFieldsRatherThanOmittingThem() throws Exception {
        var response = mvc.perform(get("/api/kkd/summary?run_id=RUN-001&unit_id=HQ"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var data = new ObjectMapper().readTree(response);
        assertThat(data.has("compared_to") && data.get("compared_to").isNull()).isTrue();
        assertThat(data.has("delta") && data.get("delta").isNull()).isTrue();
        data.get("metrics").forEach(m -> {
            for (String field : List.of("previous", "delta", "trend")) {
                assertThat(m.has(field) && m.get(field).isNull()).isTrue();
            }
        });
        data.get("issues_by_severity").forEach(s -> assertThat(s.get("previous").isNull()).isTrue());
    }

    @Test
    void filtersBeforePaginationAndReturnsStablePages() throws Exception {
        mvc.perform(get("/api/kkd/issues?run_id=RUN-002&unit_id=HQ&rule_code=GOAL_NO_OWNER&severity=critical&limit=1&offset=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.limit").value(1)).andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.items[0].goal_id").value("GOAL-018"))
                .andExpect(jsonPath("$.items[0].snapshot_date").value("2026-10-26"));
        mvc.perform(get("/api/kkd/issues?run_id=RUN-002&unit_id=UNIT-B&rule_code=GOAL_NO_OWNER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.limit").value(20)).andExpect(jsonPath("$.offset").value(0));
    }

    @Test
    void supportsMinorFilterAndOffsetBeyondTheEndWithoutOverflow() throws Exception {
        mvc.perform(get("/api/kkd/issues?run_id=RUN-002&severity=minor"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/api/kkd/issues?run_id=RUN-002&offset=2147483647&limit=100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returns404ForUnknownRunsAndUnits() throws Exception {
        mvc.perform(get("/api/kkd/summary?run_id=unknown&unit_id=HQ"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("RUN_NOT_FOUND"));
        mvc.perform(get("/api/kkd/issues?run_id=RUN-001&unit_id=unknown"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("UNIT_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/kkd/summary?run_id=RUN-001", "/api/kkd/summary?run_id=unknown&unit_id=",
            "/api/kkd/issues", "/api/kkd/issues?run_id=", "/api/kkd/issues?run_id=RUN-001&unit_id=",
            "/api/kkd/issues?run_id=RUN-001&rule_code=KR_NO_TARGET", "/api/kkd/issues?run_id=RUN-001&severity=CRITICAL",
            "/api/kkd/issues?run_id=RUN-001&limit=0", "/api/kkd/issues?run_id=RUN-001&limit=101",
            "/api/kkd/issues?run_id=RUN-001&limit=1.5", "/api/kkd/issues?run_id=RUN-001&offset=-1",
            "/api/kkd/issues?run_id=RUN-001&offset=2147483648", "/api/kkd/issues?run_id=unknown&limit=bad",
            "/api/kkd/issues?run_id=RUN-001&run_id=RUN-002", "/api/kkd/issues?run_id=RUN-001&unknown=x"
    })
    void invalidParametersAlwaysHave422AndUnifiedJson(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMS"))
                .andExpect(jsonPath("$.error.message").isNotEmpty());
    }

    @Test
    void unitWithoutHistoryKeepsComparedToButHasNullComparison() throws Exception {
        var current = new ArrayList<>(units.findAllByIdRunId("RUN-002"));
        current.add(new RunUnit(new RunUnitId("RUN-002", "NEW"), "New unit", "HQ"));
        when(units.findAllByIdRunId("RUN-002")).thenReturn(current);
        var response = mvc.perform(get("/api/kkd/summary?run_id=RUN-002&unit_id=NEW"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.compared_to.id").value("RUN-001"))
                .andReturn().getResponse().getContentAsString();
        var data = new ObjectMapper().readTree(response);
        assertThat(data.has("delta") && data.get("delta").isNull()).isTrue();
        assertThat(data.get("metrics").get(0).get("previous").isNull()).isTrue();
    }

    @Test
    void preservesMethodNotAllowedAndHidesInternalErrorDetails() throws Exception {
        mvc.perform(post("/api/runs")).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
        when(runs.findAll()).thenThrow(new IllegalStateException("secret SQL details"));
        mvc.perform(get("/api/runs")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret SQL"))));
    }
}
