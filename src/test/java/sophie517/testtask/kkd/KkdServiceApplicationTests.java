package sophie517.testtask.kkd;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sophie517.testtask.kkd.domain.repository.GoalRepository;
import sophie517.testtask.kkd.domain.repository.IssueRepository;
import sophie517.testtask.kkd.domain.repository.KeyResultRepository;
import sophie517.testtask.kkd.domain.repository.RunGoalRepository;
import sophie517.testtask.kkd.domain.repository.RunRepository;
import sophie517.testtask.kkd.domain.repository.RunUnitRepository;
import sophie517.testtask.kkd.domain.repository.UnitRepository;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
@MockitoBean(types = {UnitRepository.class, GoalRepository.class, KeyResultRepository.class,
    RunRepository.class, RunUnitRepository.class, RunGoalRepository.class, IssueRepository.class})
@AutoConfigureMockMvc
class KkdServiceApplicationTests {
    @Autowired MockMvc mvc;

    @Test
    void contextLoads() {
    }

    @Test
    void openApiDescribesAllEndpointsAndQueryParameters() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths.length()").value(3))
                .andExpect(jsonPath("$.paths['/api/runs'].get").exists())
                .andExpect(jsonPath("$.paths['/api/kkd/summary'].get.parameters[?(@.name=='run_id')].required").value(hasItem(true)))
                .andExpect(jsonPath("$.paths['/api/kkd/summary'].get.parameters[?(@.name=='unit_id')].required").value(hasItem(true)))
                .andExpect(jsonPath("$.paths['/api/kkd/issues'].get.parameters.length()").value(6))
                .andExpect(jsonPath("$.paths['/api/kkd/issues'].get.responses['422']").exists());
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}
