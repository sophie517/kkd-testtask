# Examples

Примеры структуры кода и ответов API.

## Пример ответа GET /api/kkd/summary

```json
{
  "run": { "id": "RUN-008", "snapshot_date": "2026-10-26" },
  "compared_to": { "id": "RUN-007", "snapshot_date": "2026-10-19" },
  "unit": { "id": "HQ-CIB", "name": "Департамент корпоративного бизнеса" },
  "metrics": [
    {
      "key": "clean_goals_share",
      "title": "Доля корректных целей",
      "unit": "%",
      "value": 56.5,
      "previous": 62.7,
      "delta": -6.2,
      "trend": "worse"
    },
    {
      "key": "active_goals_count",
      "title": "Количество активных целей",
      "unit": "count",
      "value": 15,
      "previous": 14,
      "delta": 1,
      "trend": "better"
    },
    {
      "key": "goals_with_issues_count",
      "title": "Количество целей с проблемами",
      "unit": "count",
      "value": 7,
      "previous": 5,
      "delta": 2,
      "trend": "worse"
    }
  ],
  "issues_by_severity": [
    { "code": "critical", "current": 2, "previous": 2, "delta": 0 },
    { "code": "major", "current": 23, "previous": 15, "delta": 8 }
  ],
  "delta": { "new": 10, "persisting": 33, "resolved": 2 }
}
```

## Пример ответа при первом прогоне

```json
{
  "run": { "id": "RUN-001", "snapshot_date": "2026-10-01" },
  "compared_to": null,
  "unit": { "id": "HQ", "name": "Штаб" },
  "metrics": [
    {
      "key": "clean_goals_share",
      "title": "Доля корректных целей",
      "unit": "%",
      "value": 80.0,
      "previous": null,
      "delta": null,
      "trend": null
    }
  ],
  "issues_by_severity": [
    { "code": "critical", "current": 1, "previous": null, "delta": null },
    { "code": "major", "current": 5, "previous": null, "delta": null }
  ],
  "delta": { "new": null, "persisting": null, "resolved": null }
}
```

## Пример ошибки

```json
{
  "error": {
    "code": "RUN_NOT_FOUND",
    "message": "Run with id 'RUN-999' not found"
  }
}
```

## Пример структуры Validation Engine

```java
// Интерфейс правила
public interface KkdRule {
    String getCode();
    Severity getSeverity();
    List<Issue> validate(ValidationContext context);
}

// Движок
public class KkdRuleEngine {
    private final List<KkdRule> rules;

    public List<Issue> execute(ValidationContext context) {
        return rules.stream()
            .flatMap(rule -> rule.validate(context).stream())
            .toList();
    }
}
```
