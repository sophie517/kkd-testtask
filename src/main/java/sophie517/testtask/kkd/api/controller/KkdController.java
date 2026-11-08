package sophie517.testtask.kkd.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sophie517.testtask.kkd.api.QueryParameters;
import sophie517.testtask.kkd.api.dto.KkdResponses;
import sophie517.testtask.kkd.api.exception.InvalidParamsException;
import sophie517.testtask.kkd.application.service.KkdQueryService;
import sophie517.testtask.kkd.application.service.KkdSummaryService;
import sophie517.testtask.kkd.domain.model.Severity;

@RestController
@RequestMapping("/api")
@Tag(name = "ККД", description = "Прогоны, метрики качества и найденные проблемы")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Успешный ответ", useReturnTypeSchema = true),
    @ApiResponse(responseCode = "404", description = "Прогон или подразделение не найдено",
            content = @Content(schema = @Schema(implementation = KkdResponses.ErrorResponse.class))),
    @ApiResponse(responseCode = "422", description = "Некорректные параметры запроса",
            content = @Content(schema = @Schema(implementation = KkdResponses.ErrorResponse.class)))
})
@RequiredArgsConstructor
public class KkdController {
    private static final Set<String> RULES = Set.of("GOAL_NO_OWNER", "GOAL_NO_KR", "KR_WEIGHTS_SUM", "CASCADE_NO_PARENT");
    private final KkdSummaryService summaries;
    private final KkdQueryService queries;

    @GetMapping("/runs")
    @Operation(summary = "Список прогонов", description = "Дата по убыванию, затем ID по возрастанию")
    public List<KkdResponses.RunResponse> runs(@Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parameters) {
        new QueryParameters(parameters, Set.of());
        return queries.listRuns().stream().map(KkdResponses::from).toList();
    }

    @GetMapping("/kkd/summary")
    @Operation(summary = "Метрики и дельта подразделения", description = "Включает дочерние подразделения исторического снимка. У первого прогона сравнение равно null.")
    @Parameters({
        @Parameter(name = "run_id", in = ParameterIn.QUERY, required = true, example = "RUN-002", schema = @Schema(type = "string", maxLength = 100)),
        @Parameter(name = "unit_id", in = ParameterIn.QUERY, required = true, example = "HQ", schema = @Schema(type = "string", maxLength = 100))
    })
    public KkdResponses.SummaryResponse summary(@Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parameters) {
        var input = new QueryParameters(parameters, Set.of("run_id", "unit_id"));
        String runId = input.required("run_id");
        String unitId = input.required("unit_id");
        return KkdResponses.from(summaries.getSummary(runId, unitId));
    }

    @GetMapping("/kkd/issues")
    @Operation(summary = "Проблемы прогона", description = "Совместные фильтры применяются до пагинации; total — число всех совпадений.")
    @Parameters({
        @Parameter(name = "run_id", in = ParameterIn.QUERY, required = true, example = "RUN-002", schema = @Schema(type = "string", maxLength = 100)),
        @Parameter(name = "unit_id", in = ParameterIn.QUERY, description = "Подразделение и его потомки", schema = @Schema(type = "string", maxLength = 100)),
        @Parameter(name = "rule_code", in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {"GOAL_NO_OWNER", "GOAL_NO_KR", "KR_WEIGHTS_SUM", "CASCADE_NO_PARENT"})),
        @Parameter(name = "severity", in = ParameterIn.QUERY, schema = @Schema(type = "string", allowableValues = {"critical", "major", "minor"})),
        @Parameter(name = "limit", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
        @Parameter(name = "offset", in = ParameterIn.QUERY, schema = @Schema(type = "integer", defaultValue = "0", minimum = "0", maximum = "2147483647"))
    })
    public KkdResponses.IssuePage issues(@Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parameters) {
        var input = new QueryParameters(parameters, Set.of("run_id", "unit_id", "rule_code", "severity", "limit", "offset"));
        String runId = input.required("run_id");
        String unitId = input.optional("unit_id");
        String ruleCode = input.optional("rule_code");
        if (ruleCode != null && !RULES.contains(ruleCode)) throw new InvalidParamsException("Unknown rule_code");
        String severityCode = input.optional("severity");
        if (severityCode != null && !Set.of("critical", "major", "minor").contains(severityCode)) {
            throw new InvalidParamsException("Unknown severity");
        }
        Severity severity = severityCode == null ? null : Severity.valueOf(severityCode.toUpperCase(Locale.ROOT));
        int limit = input.integer("limit", 20, 1, 100);
        int offset = input.integer("offset", 0, 0, Integer.MAX_VALUE);
        return KkdResponses.from(queries.listIssues(runId, unitId, ruleCode, severity, limit, offset));
    }
}
