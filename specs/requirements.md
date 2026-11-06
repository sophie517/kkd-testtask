# specs/requirements.md

## Функциональные требования

### FR-01: Подразделения
Сервис должен поддерживать иерархическую структуру подразделений с родительскими и дочерними связями. Метрика подразделения всегда включает данные дочерних подразделений.

### FR-02: Цели
Сервис должен хранить цели с полями: `owner_id` (nullable), `status` (ACTIVE / ARCHIVED), `deadline`, `unit_id`, `parent_goal_id`.

### FR-03: Ключевые результаты
Сервис должен хранить ключевые результаты для целей с полями: `target_value`, `actual_value`, `weight` (Integer, 2 знака после запятой), `actual_update_date`.

### FR-04: Прогон проверок
Сервис должен поддерживать запуск ручного прогона проверок для всего дерева подразделений. Каждый прогон сохраняет моментальный снимок (snapshot) с датой и результатами.

### FR-05: Правила проверки
Сервис должен проверять активные цели по 4 правилам:

| Код | Описание | Важность |
|---|---|---|
| `GOAL_NO_OWNER` | Цель без ответственного (`owner_id` — null) | critical |
| `GOAL_NO_KR` | У активной цели нет ни одного ключевого результата | critical |
| `KR_WEIGHTS_SUM` | Сумма весов КР цели ≠ 100 | major |
| `CASCADE_NO_PARENT` | Цель не связана с целью вышестоящего уровня | major |

**Бизнес-правила:**
- Все правила применяются **только к активным целям**.
- `CASCADE_NO_PARENT`: родительская цель — цель из непосредственного родительского подразделения. Цели корневого подразделения освобождены.
- `KR_WEIGHTS_SUM`: используется `Integer`, сумма должна строго равняться 100. Значение 99.99 — нарушение.
- Если у цели нет КР, регистрируется только `GOAL_NO_KR`, не создаётся дублирующее нарушение `KR_WEIGHTS_SUM`.

### FR-06: Метрики
Сервис должен рассчитывать 3 метрики по подразделению (включая дочерние):

| Ключ | Описание | Единица |
|---|---|---|
| `clean_goals_share` | Доля корректных целей (без нарушений) | % |
| `active_goals_count` | Количество активных целей | count |
| `goals_with_issues_count` | Количество целей с хотя бы одним нарушением | count |

Формат метрики: `key`, `title`, `unit`, `value`, `previous`, `delta`, `trend` (better / worse / neutral).

### FR-07: Дельта
Сервис должен рассчитывать дельту с предыдущим прогоном. Идентичность нарушения определяется парой `rule_code + object_id`. Три сценария:
- **new** — нарушение есть в текущем прогоне, но отсутствует в предыдущем.
- **persisting** — нарушение присутствует в обоих прогонах.
- **resolved** — нарушение было в предыдущем прогоне, но отсутствует в текущем.

Для первого прогона `compared_to`, `previous` и `delta` равны `null`.

### FR-08: Проблемы
Сервис должен хранить проблемы — нарушения правил в каждом прогоне. Поддерживать фильтрацию по подразделению, правилу и важности с пагинацией (`limit`, `offset`, `total`).

### FR-09: Обработка ошибок
- Неизвестный `run_id` → HTTP 404.
- Неизвестный `unit_id` → HTTP 404.
- Некорректные параметры → HTTP 422.
- Единый формат ошибки: `{"error": {"code": "...", "message": "..."}}`.

---

## Критерии приёмки

### AC-01: Unit-тесты Validation Engine
- Каждое из 4 правил тестируется на обнаружение нарушения при некорректных данных.
- Каждое из 4 правил тестируется на отсутствие нарушения при корректных данных.
- Правила применяются только к активным целям — архивные цели не проверяются.
- Граничные случаи проверены минимум для двух правил.

### AC-02: Тестирование дельты
- Логика расчёта дельты тестируется отдельно от Service Layer.
- Проверены три сценария: new, persisting, resolved.
- Проверена интеграция дельты с Service Layer.

### AC-03: HTTP-тесты
- Успешный запрос API → 200 OK.
- Неизвестный `run_id` → 404.
- Неизвестный `unit_id` → 404.
- Некорректные параметры → 422.
- Формат JSON ошибки проверен.

### AC-04: Liquibase
- Последовательность `update → rollback → update` проходит успешно.
- Структура БД после повторного `update` соответствует актуальной версии миграций.

### AC-05: Сборка и запуск
- `mvnw clean package` завершается успешно.
- `mvnw test` — все тесты проходят.
- `docker compose up` — приложение запускается, Liquibase применяет миграции автоматически.

---

## API-контракты

### GET /api/runs

Описание: Список прогонов с метаданными.

Response 200:
```json
[
  {
    "id": "RUN-008",
    "snapshot_date": "2026-10-26",
    "compared_to_run_id": "RUN-007",
    "issue_count": 25
  }
]
```

### GET /api/kkd/summary?run_id=&unit_id=

Описание: Сводка по подразделению с метриками, разбивкой по важности и дельтой.

Query parameters:
- `run_id` (required) — ID прогона
- `unit_id` (required) — ID подразделения

Response 200:
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
    }
  ],
  "issues_by_severity": [
    { "code": "critical", "current": 2, "previous": 2, "delta": 0 },
    { "code": "major", "current": 23, "previous": 15, "delta": 8 }
  ],
  "delta": { "new": 10, "persisting": 33, "resolved": 2 }
}
```

Response 404 (unknown run):
```json
{ "error": { "code": "RUN_NOT_FOUND", "message": "Run with id 'RUN-999' not found" } }
```

Response 404 (unknown unit):
```json
{ "error": { "code": "UNIT_NOT_FOUND", "message": "Unit with id 'UNIT-999' not found" } }
```

Response 422 (invalid params):
```json
{ "error": { "code": "INVALID_PARAMS", "message": "run_id and unit_id are required" } }
```

Для первого прогона `compared_to` — `null`, `previous` и `delta` у метрик — `null`.

### GET /api/kkd/issues

Описание: Список проблем прогона с фильтрами и пагинацией.

Query parameters:
- `run_id` (required) — ID прогона
- `unit_id` (optional) — фильтр по подразделению
- `rule_code` (optional) — фильтр по правилу
- `severity` (optional) — фильтр по важности (critical / major)
- `limit` (optional, default 20) — размер страницы
- `offset` (optional, default 0) — смещение

Response 200:
```json
{
  "items": [
    {
      "id": "ISSUE-001",
      "run_id": "RUN-008",
      "rule_code": "GOAL_NO_OWNER",
      "severity": "critical",
      "goal_id": "GOAL-042",
      "snapshot_date": "2026-10-26"
    }
  ],
  "total": 25,
  "limit": 20,
  "offset": 0
}
```

Response 404 (unknown run):
```json
{ "error": { "code": "RUN_NOT_FOUND", "message": "Run with id 'RUN-999' not found" } }
```
