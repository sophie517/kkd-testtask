# Модель данных и демо

Реализована структура этапа `domain-model`. Контейнерный запуск, применение схемы
в PostgreSQL, Hibernate validate и полный rollback проверены на финальной приёмке.
Здесь описана схема; фактические результаты применения — в [отчёте](verification.md).

## Таблицы

Все строковые ID — `VARCHAR(100)`; генерировать значения должно приложение.
Дата — `DATE` / `LocalDate`, десятичные значения — `BigDecimal`.
JPA-модели находятся в `domain/model`; связи представлены ID, а не lazy-графом Entity.
Это позволяет явно читать нужные данные и затем формировать records для чистого движка.

| Таблица | Ключ и поля | Связи |
|---|---|---|
| unit | id PK, name VARCHAR(200), nullable parent_unit_id | Родитель → unit, RESTRICT |
| goal | id PK, nullable owner_id, status, deadline, unit_id, nullable parent_goal_id | unit → unit; родитель → goal; RESTRICT |
| key_result | id PK, goal_id, nullable target_value/actual_value NUMERIC(19,2), weight NUMERIC(10,2), nullable actual_update_date | goal → goal, CASCADE |
| kkd_rule | code PK, description VARCHAR(500), severity | Каталог четырёх правил |
| run | id PK, status, snapshot_date, nullable previous_run_id | Предыдущий → run, RESTRICT |
| run_unit | PK(run_id,unit_id), name, nullable parent_unit_id | run → run CASCADE; (run_id,parent_unit_id) → run_unit NO ACTION |
| run_goal | PK(run_id,goal_id), unit_id | run → run CASCADE; (run_id,unit_id) → run_unit NO ACTION |
| issue | id PK, run_id, rule_code, goal_id, severity | run → run CASCADE; rule_code → kkd_rule RESTRICT; (run_id,goal_id) → run_goal CASCADE |

Все поля обязательны, кроме явно помеченных nullable.
`UNIQUE(run_id,rule_code,goal_id)` запрещает повтор одной проблемы в прогоне.
CHECK ограничивают статусы ACTIVE/ARCHIVED и COMPLETED, важность critical/major/minor,
неотрицательный вес, ссылки подразделений/целей на себя и ссылку прогона на себя.
Полные циклы и единственный корень проверяются подготовкой данных и будущим сервисом запуска.

`NO ACTION` внутри снимка проверяет связи к концу SQL-операции и позволяет удалять
весь прогон каскадом, не получая промежуточных конфликтов родителя и потомков.
Это намеренное отличие от RESTRICT для ссылок текущих данных.
Исторические unit_id/goal_id не имеют FK на текущие unit/goal: история независима
от изменения текущих данных. Метрики не должны читать текущую структуру дерева.

Вес хранится непосредственно в процентах: `50.00` означает 50%, без масштабирования.
В JPA GoalStatus/RunStatus используют строковые значения enum; SeverityConverter
сохраняет важность в нижнем регистре, как в БД и API.

## Миграции

Корень: [changelog.yml](../src/main/resources/db/changelog/changelog.yml).

| changeSet | Содержание | Откат |
|---|---|---|
| 001-current-schema | Четыре таблицы текущих данных | Удаление таблиц в обратном порядке |
| 002-history-schema | Четыре таблицы истории | issue → run_goal → run_unit → run |
| 003-rule-catalog | Четыре выбранных правила | Удаление записей по их кодам |
| 004-demo-data | Текущие данные и два снимка | RUN-002 → RUN-001, разрыв родительских ссылок демо-целей, удаление демо-целей и подразделений |

Данные загружаются Liquibase `loadData` из UTF-8 CSV в `db/changelog/data`.
`NULL` в CSV означает SQL NULL. Пробельный owner_id у GOAL-003 второго снимка
сохранён намеренно: он должен считаться отсутствием владельца.
Rollback seed ограничен конкретными demo ID; не удаляет произвольные пользовательские записи.
Если на демо-объекты позже появились внешние ссылки, откат может быть заблокирован FK;
полную проверку выполнять только на отдельной БД, как требует план приёмки.

## Демонстрационный набор

HQ — корень; UNIT-A и UNIT-B — дочерние. По 8 целей в каждом подразделении.
GOAL-008, GOAL-016, GOAL-024 архивные: у них намеренно отсутствуют owner и КР,
но проблем для них нет. Всего 24 цели, 21 активная, 40 КР в каждом входном наборе.
У GOAL-018 одновременно два нарушения; число проблем не равно числу проблемных целей.

Полные воспроизводимые входы:
[RUN-001](../src/test/resources/fixtures/run-001.json) и
[RUN-002](../src/test/resources/fixtures/run-002.json).
Текущие unit/goal/key_result соответствуют второму набору.
Ожидаемые проблемы перечислены в expectedIssues каждого JSON и
[issue.csv](../src/main/resources/db/changelog/data/issue.csv).

| Показатель по HQ с потомками | RUN-001: 2026-10-19 | RUN-002: 2026-10-26 |
|---|---|---|
| compared_to | null | RUN-001 |
| Активных целей | 21 | 21 |
| Целей с проблемами | 8 | 7 |
| Проблем | 9 | 8 |
| critical / major / minor | 3 / 6 / 0 | 3 / 5 / 0 |
| Доля корректных целей | 61.9% | 66.7% |
| Новые / сохраняющиеся / исправленные | null | 4 / 4 / 5 |

Из первого прогона исправлены: GOAL-001/GOAL_NO_OWNER, GOAL-009/GOAL_NO_KR,
GOAL-010/KR_WEIGHTS_SUM, GOAL-019/KR_WEIGHTS_SUM, GOAL-020/CASCADE_NO_PARENT.
Новые: GOAL-003/GOAL_NO_OWNER, GOAL-011/GOAL_NO_KR,
GOAL-012/KR_WEIGHTS_SUM, GOAL-022/CASCADE_NO_PARENT.
Остаются: GOAL-017/CASCADE_NO_PARENT, GOAL-018/GOAL_NO_OWNER,
GOAL-018/KR_WEIGHTS_SUM, GOAL-021/CASCADE_NO_PARENT.

Это эталон данных. На этапе validation-engine движок должен независимо получить
эти нарушения из двух входов; текущие тесты не реализуют правила вместо движка.

## Что проверено без БД

`./mvnw test`: компиляция сущностей и тесты без БД, включая:
- построение и проверку Hibernate metadata восьми сущностей без JDBC-доступа;
- разбор всего Liquibase root, генерацию PostgreSQL SQL и наличие rollback;
- совпадение текущих CSV с fixture второго прогона;
- полноту снимков всех активных целей и ожидаемых проблем;
- корректность дерева fixture и категории изменений 4/4/5.

Эти проверки не доказывают применение ограничений PostgreSQL и Hibernate validate.
Позже, на изолированном стенде, проверить update, повторный update, полный rollback,
повторный update, фактические ограничения и каскадное удаление.
После запуска для подсчётов можно выполнить:

```sql
SELECT count(*) FROM unit;       -- 3
SELECT count(*) FROM goal;       -- 24
SELECT count(*) FROM key_result; -- 40
SELECT run_id, count(*) FROM run_goal GROUP BY run_id ORDER BY run_id; -- 21, 21
SELECT run_id, count(*) FROM issue GROUP BY run_id ORDER BY run_id;    -- 9, 8
```
