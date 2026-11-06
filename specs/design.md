# specs/design.md

## Модель данных

### Entity: Unit

| Поле | Тип | Описание |
|---|---|---|
| `id` | String (PK) | Уникальный идентификатор подразделения |
| `name` | String | Название подразделения |
| `parent_unit_id` | String (FK) | Ссылка на родительское подразделение (nullable) |

### Entity: Goal

| Поле | Тип | Описание |
|---|---|---|
| `id` | String (PK) | Уникальный идентификатор цели |
| `owner_id` | String (nullable) | ID ответственного сотрудника |
| `status` | Enum (ACTIVE, ARCHIVED) | Статус цели |
| `deadline` | LocalDate | Срок выполнения |
| `unit_id` | String (FK) | Подразделение |
| `parent_goal_id` | String (nullable, FK) | Родительская цель |

### Entity: KeyResult

| Поле | Тип | Описание |
|---|---|---|
| `id` | String (PK) | Уникальный идентификатор |
| `goal_id` | String (FK) | Цель |
| `target_value` | Integer | Целевое значение |
| `actual_value` | Integer | Фактическое значение |
| `weight` | Integer | Вес (2 знака после запятой) |
| `actual_update_date` | LocalDate | Дата обновления факта |

### Entity: Run

| Поле | Тип | Описание |
|---|---|---|
| `id` | String (PK) | Уникальный идентификатор прогона |
| `snapshot_date` | LocalDate | Дата снимка данных |
| `previous_run_id` | String (nullable, FK) | Ссылка на предыдущий прогон |

### Entity: Issue

| Поле | Тип | Описание |
|---|---|---|
| `id` | String (PK) | Уникальный идентификатор |
| `run_id` | String (FK) | Прогон |
| `rule_code` | String | Код правила |
| `severity` | Enum (critical, major) | Важность |
| `goal_id` | String | Цель (объект нарушения) |
| `snapshot_date` | LocalDate | Дата снимка |

### Примечания

- `owner_id` хранит только ID сотрудника, не персональные данные.
- `weight` — Integer (не float/double), строго 2 знака после запятой.
- `rule_code` в Issue — строковый код, каталог правил хранится в коде (TBD: согласовать хранение в БД).

---

## Архитектура

### Слои

```
┌─────────────────────────────────────────┐
│         API Layer                       │  REST Controllers, DTO, Exception
│         (api/controller, api/dto)       │
├─────────────────────────────────────────┤
│       Application / Service Layer       │  RunService, KkdSummaryService,
│       (application/service)             │  оркестрация, транзакции, сравнение
├─────────────────────────────────────────┤
│   Validation Engine                     │  KkdRule, KkdRuleEngine,
│   (validation/)                         │  4 правила, чистый Java
├─────────────────────────────────────────┤
│     Repository Layer                    │  Spring Data JPA
│     (domain/repository)                 │
├─────────────────────────────────────────┤
│         Database                        │  PostgreSQL 15+, Liquibase
│         (src/main/resources/db/)        │
└─────────────────────────────────────────┘
```

### Структура пакетов

```
sophie517.testtask.kkd
├── KkdServiceApplication.java
├── api/
│   ├── controller/          — REST-контроллеры
│   ├── dto/                 — Request/Response DTO
│   └── exception/           — обработка HTTP-ошибок
├── application/
│   └── service/             — RunService, KkdSummaryService, KkdIssueService
├── domain/
│   ├── model/               — JPA Entity (Unit, Goal, KeyResult, Run, Issue)
│   └── repository/          — Spring Data JPA Repository
├── validation/
│   ├── KkdRule.java         — интерфейс правила
│   ├── KkdRuleEngine.java   — движок применения правил
│   ├── ValidationContext.java — контекст данных для правил
│   └── impl/                — 4 реализации правил
└── shared/                  — действительно общие компоненты
```

### Архитектурные инварианты

1. **Validation Engine — чистый компонент.** Не зависит от Spring, JPA и базы данных. Получает подготовленные данные через `ValidationContext` и возвращает список найденных нарушений.
2. **Контроллеры не обращаются к репозиториям напрямую.** Путь: Controller → Service → Repository.
3. **JPA Entity и API DTO разделены.** Entity не используются напрямую в API.
4. **Сравнение прогонов — в Service Layer,** а не в Validation Engine.
5. **Правила проверяются независимо от Spring-контекста.** Каждый Rule — чистый Java-класс.

---

## API

### REST-контроллеры

| Controller | Метод | Endpoint | Описание |
|---|---|---|---|
| `RunController` | GET | `/api/runs` | Список прогонов |
| `KkdSummaryController` | GET | `/api/kkd/summary` | Сводка по подразделению |
| `KkdIssueController` | GET | `/api/kkd/issues` | Список проблем |

### Обработка ошибок

| Код ошибки | HTTP-статус | Описание |
|---|---|---|
| `RUN_NOT_FOUND` | 404 | Прогон не найден |
| `UNIT_NOT_FOUND` | 404 | Подразделение не найдено |
| `INVALID_PARAMS` | 422 | Некорректные параметры запроса |

Формат ошибки:
```json
{"error": {"code": "...", "message": "..."}}
```

---

## Ключевые технические решения

### Решение 1: Validation Engine как чистый Java-компонент

**Проблема:** Правила проверки должны быть тестируемыми без Spring-контекста и базы данных.

**Решение:** Validation Engine — чистый Java, без Spring-аннотаций. Получает `ValidationContext` (DTO с подготовленными данными) и возвращает `List<Issue>`.

**Обоснование:**
- Правила тестируются как unit-тесты без загрузки Spring-контекста.
- Нет скрытых зависимостей — входные данные явно передаются через `ValidationContext`.
- Новое правило добавляется как реализация `KkdRule`.

**Альтернативы:**
- Правила как Spring Beans — сложнее тестирование, скрытые зависимости.
- Правила как методы Service Layer — смешивание логики проверок с оркестрацией.

### Решение 2: Integer для весов КР

**Проблема:** Вес КР должен быть точным и легко сравниваться с 100.

**Решение:** Использовать `Integer` (не `float`/`double`) с 2 знаками после запятой. Сумма должна строго равняться 100.

**Обоснование:**
- Нет проблем с плавающей точкой (99.999999 ≠ 100).
- Простое сравнение: `sum == 100`.
- Значение 99.99 — явное нарушение.

**Альтернативы:**
- `BigDecimal` — избыточно для простого сравнения с 100.
- `double` — проблемы с точностью (99.999999999 ≠ 100).

### Решение 3: DTO, Entity и модели Validation Engine разделены

**Проблема:** JPA Entity не должны попадать в API и в бизнес-логику.

**Решение:** Три уровня моделей:
1. JPA Entity — для работы с базой данных.
2. API DTO — для передачи данных через HTTP.
3. Validation models — для передачи данных в Validation Engine.

**Обоснование:**
- Entity не знает о HTTP-контрактах.
- DTO не знает о JPA-аннотациях.
- Validation Engine не зависит от Spring.

**Альтернативы:**
- Использование Entity как DTO — утечка структуры БД в API, привязка к JPA.

### Решение 4: Liquibase с rollback

**Проблема:** Миграции должны поддерживать накат и повторный накат без ошибок.

**Решение:** Liquibase changelog с `<rollback>` секциями для каждой миграции.

**Обоснование:**
- Условие приёмки тестового задания.
- Безопасное откатывание при ошибках.
- Повторный накат без конфликтов.

---

## TBD

| Решение | Статус |
|---|---|
| Хранение каталога правил в БД vs код | Требуется согласование. В v1 правила хранятся в коде. |
