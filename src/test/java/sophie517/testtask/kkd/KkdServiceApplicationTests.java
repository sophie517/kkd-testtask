package sophie517.testtask.kkd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
class KkdServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
