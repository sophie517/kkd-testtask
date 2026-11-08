# Согласованные примеры API

Обновлено: 2026-11-08. Примеры иллюстрируют контракт; это не готовый seed.
Идентификаторы передаются строками без преобразования.

## Сводка второго прогона

В поддереве UNIT-A в обоих прогонах по 20 активных целей.
В первом 8 целей с 10 проблемами; во втором 5 целей с 8 проблемами.
Доля корректных целей: 60.0% → 75.0%.
Из проблем второго прогона 3 новые и 5 сохраняющихся, исправлено 5.
8 = 3 + 5; 10 = 5 + 5. Разбивка по важности даёт те же суммы.

```json
{
  "run": {
    "id": "RUN-002",
    "snapshot_date": "2026-10-26"
  },
  "compared_to": {
    "id": "RUN-001",
    "snapshot_date": "2026-10-19"
  },
  "unit": {
    "id": "UNIT-A",
    "name": "Подразделение А"
  },
  "metrics": [
    {
      "key": "clean_goals_share",
      "title": "Доля корректных целей",
      "unit": "%",
      "value": 75,
      "previous": 60,
      "delta": 15,
      "trend": "better"
    },
    {
      "key": "goals_with_issues_count",
      "title": "Количество целей с проблемами",
      "unit": "count",
      "value": 5,
      "previous": 8,
      "delta": -3,
      "trend": "better"
    },
    {
      "key": "issues_count",
      "title": "Количество проблем",
      "unit": "count",
      "value": 8,
      "previous": 10,
      "delta": -2,
      "trend": "better"
    }
  ],
  "issues_by_severity": [
    {
      "code": "critical",
      "current": 2,
      "previous": 4,
      "delta": -2
    },
    {
      "code": "major",
      "current": 6,
      "previous": 6,
      "delta": 0
    },
    {
      "code": "minor",
      "current": 0,
      "previous": 0,
      "delta": 0
    }
  ],
  "delta": {
    "new": 3,
    "persisting": 5,
    "resolved": 5
  }
}
```

## Сводка первого прогона

```json
{
  "run": {
    "id": "RUN-001",
    "snapshot_date": "2026-10-19"
  },
  "compared_to": null,
  "unit": {
    "id": "UNIT-A",
    "name": "Подразделение А"
  },
  "metrics": [
    {
      "key": "clean_goals_share",
      "title": "Доля корректных целей",
      "unit": "%",
      "value": 60,
      "previous": null,
      "delta": null,
      "trend": null
    },
    {
      "key": "goals_with_issues_count",
      "title": "Количество целей с проблемами",
      "unit": "count",
      "value": 8,
      "previous": null,
      "delta": null,
      "trend": null
    },
    {
      "key": "issues_count",
      "title": "Количество проблем",
      "unit": "count",
      "value": 10,
      "previous": null,
      "delta": null,
      "trend": null
    }
  ],
  "issues_by_severity": [
    {
      "code": "critical",
      "current": 4,
      "previous": null,
      "delta": null
    },
    {
      "code": "major",
      "current": 6,
      "previous": null,
      "delta": null
    },
    {
      "code": "minor",
      "current": 0,
      "previous": null,
      "delta": null
    }
  ],
  "delta": null
}
```

## Список прогонов

В этом примере все проблемы принадлежат поддереву UNIT-A,
поэтому общие issue_count совпадают с его метриками.

```json
[
  {
    "id": "RUN-002",
    "status": "COMPLETED",
    "snapshot_date": "2026-10-26",
    "compared_to_run_id": "RUN-001",
    "issue_count": 8
  },
  {
    "id": "RUN-001",
    "status": "COMPLETED",
    "snapshot_date": "2026-10-19",
    "compared_to_run_id": null,
    "issue_count": 10
  }
]
```

## Страница проблем

GET /api/kkd/issues?run_id=RUN-002&rule_code=GOAL_NO_OWNER&limit=1&offset=0

```json
{
  "items": [
    {
      "id": "ISSUE-021",
      "run_id": "RUN-002",
      "rule_code": "GOAL_NO_OWNER",
      "severity": "critical",
      "goal_id": "GOAL-007",
      "snapshot_date": "2026-10-26"
    }
  ],
  "total": 2,
  "limit": 1,
  "offset": 0
}
```

## Ошибка

HTTP 404 для неизвестного прогона:

```json
{"error": {"code": "RUN_NOT_FOUND", "message": "Run 'RUN-999' not found"}}
```

Текст message может быть другим; code, HTTP-статус и структура error стабильны.
