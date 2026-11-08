# Validation Engine

Чистый Java-компонент `validation/KkdRuleEngine`, без Spring, JPA Entity,
обращений к БД, системному времени и сохранения результатов.
Переиспользуются только обычные enum GoalStatus и Severity из доменной модели.

Вход — `ValidationContext` с immutable records UnitData, GoalData и KeyResultData.
Коллекции копируются при создании контекста. Передаются все цели, включая архивных
родителей; движок проверяет только ACTIVE. Для 24 целей достаточно поиска в списках.

```java
var context = new ValidationContext(
    List.of(new UnitData("HQ", null)),
    List.of(new GoalData("GOAL-001", "EMP-001", GoalStatus.ACTIVE, "HQ", null)),
    List.of(new KeyResultData("GOAL-001", new BigDecimal("100.00")))
);
List<Violation> issues = new KkdRuleEngine().execute(context); // пустой список
```

Точки входа и модели находятся в `sophie517.testtask.kkd.validation` и `.validation.model`.
Результат — immutable список Violation(ruleCode, goalId, severity), упорядоченный
по ruleCode, затем goalId, независимо от порядка входных коллекций.
runId и ID сохраняемой проблемы присваивает будущий сервис запуска.

| Правило | Условие |
|---|---|
| GOAL_NO_OWNER / critical | ownerId равен null или isBlank() |
| GOAL_NO_KR / critical | У активной цели нет КР |
| KR_WEIGHTS_SUM / major | КР есть, точная сумма весов не равна 100 через BigDecimal.compareTo |
| CASCADE_NO_PARENT / major | В некорневом подразделении нет ACTIVE родителя непосредственно вышестоящего подразделения |

При отсутствии КР подавляется только проверка суммы весов; остальные правила работают.
Цели корня освобождены от CASCADE_NO_PARENT. Архивный родитель считается невалидным.
Структурно корректные данные подготавливает будущий сервис запуска; неизвестный
unitId вызывает IllegalArgumentException, а не вымышленное нарушение качества.
Каждый KkdRule получает активную цель; единая фильтрация статуса находится в движке.

Быстрая проверка без БД, HTTP и Spring-контекста:

```bash
./mvnw -Dtest=KkdRuleEngineTests test
```

16 сценариев включают граничные данные четырёх правил, исключение архивных целей,
точные дробные суммы, устойчивость к порядку коллекций и оба входных fixture.
Результаты fixture полностью совпали с эталоном этапа domain-model: 9 и 8 проблем.
Новых зависимостей и изменений миграций нет. Сохранение прогонов — следующий этап kkd-runs.
