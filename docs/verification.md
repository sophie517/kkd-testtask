# Итоги реализации и приёмки

Проверено 2026-11-08. Все обязательные критерии задания из матрицы ниже подтверждены.
Проверки выполнялись на отдельных Compose projects/volumes `kkd-final-check` и
`kkd-final-custom`; существующие БД пользователя не изменялись.

## Требования и доказательства

| Требование задания | Реализация / свидетельство | Статус |
|---|---|---|
| Подразделения с иерархией, цели и КР | domain/model, 001-current-schema.yml; JpaMappingTests | Проверено PostgreSQL: NOT NULL/FK/CHECK/UNIQUE и каскады |
| Каталог правил, прогон, проблема | kkd_rule, run, issue; 002-history-schema.yml | Проверено: восемь доменных таблиц и Hibernate validate |
| Четыре правила отдельно от API/БД | validation; KkdRuleEngineTests, 16 сценариев предыдущего этапа | Проверено без БД |
| Только активные цели и их КР | Отбор ACTIVE в движке и снимках; тесты архивных целей | Проверено без БД |
| 20–30 целей, 2–3 подразделения, два прогона | 24 цели, 3 подразделения, 2 fixture и CSV seed; DemoDataConsistencyTests | Проверено в чистой PostgreSQL: 3/24/40/2 |
| Нарушения всех выбранных правил, сравнимые прогоны | Эталоны 9/8 проблем и дельта 4/4/5; сверка движком двух fixture | Проверено без БД |
| Liquibase и обратимые миграции | Четыре changeSet с явным rollback; MigrationPlanTests генерирует SQL | Повторный update и полный rollback/update прошли; схема и все строки совпали |
| docker compose up | Dockerfile, compose.yaml, env defaults, healthcheck | Итоговый образ собран; два чистых стенда и повторный старт успешны |
| GET /api/runs | KkdController → KkdQueryService; KkdApiTests | MockMvc: порядок, статус, даты, compared_to, issue_count |
| Минимум три метрики с previous/delta/trend | SummaryCalculator, KkdSummaryService; SummaryCalculatorTests | Проверено без БД |
| Метрики включают дочерние подразделения | Исторический обход Snapshot.subtree; тесты поддеревьев и переносов | Проверено без БД |
| Разбивка по важности и новые/сохраняющиеся/исправленные | SummaryCalculator, IssueDeltaCalculator | Проверены числа и инварианты |
| GET /api/kkd/issues с фильтрами и пагинацией | KkdQueryService; KkdApiTests | MockMvc: совместные фильтры, total, страницы, большой offset |
| Идентичность по правилу и объекту | Snapshot.ProblemKey, IssueDeltaCalculator | Проверено независимо от issue.id |
| Первый прогон: previous/delta=null | DTO с явными null; calculator и MockMvc | Проверено, поля не пропускаются |
| Неизвестный прогон/подразделение — 404 | Типизированные исключения, ApiExceptionHandler | Проверено MockMvc и реальным HTTP |
| Некорректные параметры — 422 и единый error JSON | QueryParameters, ApiExceptionHandler | 15 наборов MockMvc; дополнительно реальные 422 |
| Минимум 4 JUnit-сценария: 2 правила, метрика, дельта | KkdRuleEngineTests и SummaryCalculatorTests | Проверено полным clean verify |
| Стек Java 17+, Spring Boot 3, PostgreSQL 15+, Maven | Java 21, Boot 3.5.16, postgres:15, Maven Wrapper 3.10.0 | Конфигурация готова, итоговый JAR собран |
| Конфигурация через окружение и .env.example | application.yml, compose.yaml, .env.example | Проверены demo-defaults, другие реквизиты и порты; внешний DB_URL не попал в контейнер |
| Артефакт работы с ассистентом | AI-PLAN.md | Обновлён; ручное авторство не выдумывается |

## Полная сборка и тесты

```bash
KKD_INTEGRATION_TESTS=true DB_URL=jdbc:postgresql://localhost:15440/kkd \
  DB_USERNAME=kkd DB_PASSWORD=change-me ./mvnw clean verify
```

**BUILD SUCCESS: 62 теста, 0 failures, 0 errors, 0 skipped; 6.824 с по Maven.**
Собран `target/kkd-service-0.0.1-SNAPSHOT.jar`. До добавления интеграционных тестов
обычный `clean verify` также прошёл: 59 тестов, без ошибок, 5.206 с.
В обычной сборке без KKD_INTEGRATION_TESTS три PostgreSQL-теста пропускаются.

| Набор | Тестов | Что подтверждено |
|---|---:|---|
| Spring smoke | 1 | Wiring контроллеров/сервисов с mock-репозиториями |
| KkdRuleEngineTests | 16 | Четыре правила, границы, архивные цели, оба полных fixture |
| RunServiceTests | 6 | Оркестрация, дерево, снимки, ошибки, предшественник |
| SummaryCalculatorTests | 8 | Метрики, округление, поддеревья, история и дельта |
| KkdQueryServiceTests | 1 | Список прогонов, сортировка |
| KkdApiTests | 22 | MockMvc: DTO, фильтры, пагинация, null, 404/422/405/500 |
| DemoDataConsistencyTests | 3 | CSV и fixture, ключи проблем, два сравнимых снимка |
| MigrationPlanTests | 1 | Разбор Liquibase и PostgreSQL SQL |
| JpaMappingTests | 1 | Hibernate metadata восьми сущностей |
| PostgresIntegrationTests | 3 | Реальный persistence, rollback, изоляция, FK/CHECK/UNIQUE/NOT NULL, каскады и история |

Намеренные ошибки CHECK и обработчика 500 присутствуют в журналах тестов;
это ожидаемые сценарии, а не падения тестов. JVM предупреждает о динамическом
подключении Mockito agent; на использованной Java 21 это не помешало проверкам.

## Docker, PostgreSQL и HTTP

- Итоговый multi-stage Dockerfile собрал приложение с нуля в build layer.
- Первый старт на новом volume: 4 changeSet, восемь таблиц, 4 правила,
  3 подразделения, 24 цели, 40 КР, 2 прогона, 6 run_unit, 42 run_goal, 17 issue.
- Hibernate `ddl-auto=validate` прошёл против настоящей схемы.
- Стенд с demo-defaults: DB_PORT=15440, APP_PORT=18090.
- Второй новый стенд: DB_PORT=15441, APP_PORT=18091, другой пользователь/пароль.
  Заведомо неподходящий внешний DB_URL не нарушил внутреннее подключение к db:5432.
- `scripts/check-api.py` выполнил 20 запросов на каждом стенде; на первом
  повторён после перезапуска. Все проверки успешны.
- RUN-002/HQ: 66.7%, previous 61.9%, delta +4.8 п.п.; 7 проблемных целей,
  8 проблем; critical/major/minor = 3/5/0; new/persisting/resolved = 4/4/5.
- Проверены первый прогон с явными null, общие фильтры, страницы и total,
  пустая выборка minor, большой offset, неизвестные объекты 404 и параметры 422.
- Повторный старт: 2 прогона, 24 цели, 17 проблем, 4 записи DATABASECHANGELOG;
  демо не дублируется, ответы API сохраняются.

Первый запуск с портом по умолчанию 5432 остановился: этот порт уже занят на хосте.
После выбора свободных портов стенды запустились. Это ограничение окружения;
существующий процесс на 5432 не останавливался.

## Транзакции и ограничения

PostgresIntegrationTests запускается через настоящий Spring-контекст без HTTP.
Временный CHECK на issue намеренно отклоняет новый UUID-прогон после flush
run и снимков. После исключения из Spring proxy отдельные JDBC-запросы подтверждают,
что количества всех четырёх исторических таблиц не изменились.

Успешный RunService создаёт 3 снимка подразделений, 21 активную цель и 8 проблем.
Временный PostgreSQL trigger на записи issue проверяет фактическое значение
transaction_isolation = repeatable read. Изменение имени текущего подразделения
и архивирование текущей цели не меняют сводку сохранённого RUN-002.
Удаление созданного тестового прогона удаляет его снимки и проблемы каскадом.

Проверены 11 отклонённых операций: неверные status/severity/weight,
обязательная дата NULL, отсутствующие текущие/исторические FK,
самоссылки подразделения/прогона, дубль проблемы и удаление предшественника.
Удаление временной текущей цели каскадно удалило её КР.
Тестовые изменения, trigger, функция и дополнительный CHECK удалены.

## Реальный цикл Liquibase

```bash
python3 scripts/check-migrations.py kkd-final-check
```

Использован официальный CLI `liquibase/liquibase:4.31.1`, changelog смонтирован
из src/main/resources с исходным путём db/changelog/changelog.yml.
Приложение остановлено перед циклом. Подтверждены:

1. Повторный update не меняет схему и ни одну строку demo.
2. `rollback-count --count=4` успешно откатывает все changeSet.
3. После rollback нет доменных таблиц; DATABASECHANGELOG содержит 0 записей.
4. Новый update восстанавливает все восемь таблиц и полный seed.
5. pg_dump структуры (без двух служебных таблиц) и все столбцы/строки
   восьми таблиц полностью совпадают с состоянием до rollback.

SHA256 нормализованной схемы и данных:
`6a704c217b1f446748b7c229948699f019542f477e596c6a5ad2510c21f25be9`.
После цикла приложение снова запущено и HTTP проверен.
Точная CLI-команда и повторяемые команды приведены в README.

## Окружение и границы результата

Java 21.0.9 (Microsoft), Maven Wrapper 3.10.0, Spring Boot 3.5.16,
Hibernate 6.6.53.Final, Liquibase 4.31.1, PostgreSQL 15.19,
Docker client/server 29.1.3, Compose v5.0.0-desktop.1, macOS arm64.

Нагрузочные тесты, промышленное развёртывание и проверка других версий ОС/СУБД
не выполнялись и не требуются этим заданием. Приёмка подтверждает описанные
сценарии и окружение; она не доказывает отсутствие любых возможных дефектов.
OpenSpec и участие ассистента отражены в AI-PLAN.md.

После приёмки тестовые контейнеры/сети обоих projects остановлены и удалены;
volumes сохранены. Шесть основных specs синхронизированы, все семь changes
архивированы с 70/70 выполненными задачами. Проверка локальных Markdown-ссылок
не обнаружила битых ссылок после переноса в архив.

## Дополнительная Java-проверка миграций

После основной приёмки добавлен MigrationCycleTests с отдельным флагом
KKD_MIGRATION_TESTS. Проверен на PostgreSQL отдельного проекта kkd-java-migrations:
update → повторный update → rollback четырёх changeSet → update.
Все строки восьми таблиц, определения столбцов, ограничения и индексы совпали.
Тестовая схема удалена. Результат: 1 тест, 0 failures/errors/skipped,
BUILD SUCCESS, 3.899 с. Этот результат дополняет основной прогон 62 тестов выше;
новый класс не входил в тот исторический прогон. Команда запуска — в README.

## Проверка после добавления Lombok и Swagger

Change developer-experience: README сокращён, ручные инструкции вынесены в
migrations.md, Lombok заменил шаблонные getters/конструкторы; добавлен springdoc 2.8.17.
Полный clean verify с обоими флагами KKD_INTEGRATION_TESTS/KKD_MIGRATION_TESTS:
**65 тестов, 0 failures/errors/skipped, BUILD SUCCESS, 8.380 с**, JAR собран.
Новые проверки через MockMvc подтверждают /v3/api-docs с тремя GET endpoint,
обязательными параметрами summary и шестью параметрами issues, а также
HTTP 200 и HTML по /swagger-ui/index.html. Остальные тесты включают реальную
PostgreSQL и полный цикл Liquibase. Итоговый Docker-образ в этом шаге заново
не собирался; конфигурация Compose и Dockerfile сохранена.
