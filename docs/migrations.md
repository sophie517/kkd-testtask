# Проверка миграций вручную

Команды выполняются из корня проекта.

### Миграции вручную через Docker

Выполняйте команды из корня проекта. Нужны Docker Compose и свободный порт 15442.
Используйте новое имя проекта, у которого ещё нет volume. Реквизиты заданы явно,
чтобы локальный .env не изменил настройки отдельного стенда:

```bash
DB_PORT=15442 DB_USERNAME=kkd DB_PASSWORD=change-me \
  docker compose -p kkd-manual-check up -d --wait db
```

Для удобства задайте в текущем bash/zsh короткую функцию запуска CLI.
Она только сокращает команду `docker run`; каждую операцию ниже вы вызываете вручную:

```bash
lb_docker() {
  docker run --rm --network kkd-manual-check_default \
    -v "$PWD/src/main/resources:/liquibase/changelog:ro" \
    liquibase/liquibase:4.31.1 --search-path=/liquibase/changelog \
    --changelog-file=db/changelog/changelog.yml \
    --url=jdbc:postgresql://db:5432/kkd --username=kkd --password=change-me "$@"
}
```

**1. Первый накат и проверка состояния:**

```bash
lb_docker update
lb_docker status
lb_docker history

docker compose -p kkd-manual-check exec -T db \
  psql -U kkd -d kkd -c '\dt'
```

Ожидаются восемь доменных таблиц и две служебные таблицы Liquibase,
четыре выполненных changeSet и отсутствие ожидающих изменений.
Повторный `lb_docker update` не должен применять новые changeSet или добавлять демо.

**2. Откат всех четырёх changeSet:**

```bash
lb_docker rollback-count --count=4

docker compose -p kkd-manual-check exec -T db \
  psql -U kkd -d kkd -c '\dt'
docker compose -p kkd-manual-check exec -T db \
  psql -U kkd -d kkd -c 'SELECT count(*) FROM databasechangelog;'
```

После отката остаются только две служебные таблицы, DATABASECHANGELOG пуст.
Приложение во время отката должно быть остановлено; здесь запущена только БД.

**3. Повторный накат и сверка данных:**

```bash
lb_docker update
lb_docker status

docker compose -p kkd-manual-check exec -T db psql -U kkd -d kkd -c "
  SELECT 'unit' AS entity, count(*) FROM unit
  UNION ALL SELECT 'goal', count(*) FROM goal
  UNION ALL SELECT 'key_result', count(*) FROM key_result
  UNION ALL SELECT 'kkd_rule', count(*) FROM kkd_rule
  UNION ALL SELECT 'run', count(*) FROM run
  UNION ALL SELECT 'run_unit', count(*) FROM run_unit
  UNION ALL SELECT 'run_goal', count(*) FROM run_goal
  UNION ALL SELECT 'issue', count(*) FROM issue;
"
```

Ожидаемые количества после первого и повторного наката:

| Таблица | Строк |
|---|---:|
| unit | 3 |
| goal | 24 |
| key_result | 40 |
| kkd_rule | 4 |
| run | 2 |
| run_unit | 6 |
| run_goal | 42 |
| issue | 17 |

Эта ручная сверка подтверждает успешный цикл и количества записей.
Подробные результаты ранее выполненной проверки, включая сравнение всей структуры
и содержимого таблиц, приведены в [отчёте](verification.md).

После проверки остановите отдельный стенд:

```bash
docker compose -p kkd-manual-check down
```

Volume сохраняется. `down --volumes` удаляет его данные и не заменяет Liquibase rollback.

### Миграции вручную полностью локально

Нужны JDK 21, PostgreSQL 15+, `psql` и установленный Liquibase CLI 4.31.1
с JDBC-драйвером PostgreSQL. Команды ниже рассчитаны на bash/zsh.
Создайте отдельную пустую БД `kkd_check`, как описано выше. Остановите приложение
перед проверкой; для первого наката на пустой БД пока не запускайте его.

Из корня репозитория задайте соединение:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/kkd_check
export DB_USERNAME=kkd
export DB_PASSWORD=kkd
export PGPASSWORD="$DB_PASSWORD"

lb_local() {
  liquibase --search-path=src/main/resources \
    --changelog-file=db/changelog/changelog.yml \
    --url="$DB_URL" --username="$DB_USERNAME" --password="$DB_PASSWORD" "$@"
}
```

Функция вызывает установленный локально CLI; Docker для этого варианта не нужен.
Путь `db/changelog/changelog.yml` сохраняет ту же идентичность changeSet,
которую использует Spring Boot.

**Накат:**

```bash
lb_local update
lb_local status
lb_local history
psql -h localhost -p 5432 -U "$DB_USERNAME" -d kkd_check -c '\dt'
```

Выполните также `lb_local update` ещё раз: новых миграций и дублей быть не должно.

**Откат:**

```bash
lb_local rollback-count --count=4
psql -h localhost -p 5432 -U "$DB_USERNAME" -d kkd_check -c '\dt'
psql -h localhost -p 5432 -U "$DB_USERNAME" -d kkd_check \
  -c 'SELECT count(*) FROM databasechangelog;'
```

Должны остаться две служебные таблицы и 0 записей DATABASECHANGELOG.

**Повторный накат:**

```bash
lb_local update
lb_local status
psql -h localhost -p 5432 -U "$DB_USERNAME" -d kkd_check -c "
  SELECT 'unit' AS entity, count(*) FROM unit
  UNION ALL SELECT 'goal', count(*) FROM goal
  UNION ALL SELECT 'key_result', count(*) FROM key_result
  UNION ALL SELECT 'kkd_rule', count(*) FROM kkd_rule
  UNION ALL SELECT 'run', count(*) FROM run
  UNION ALL SELECT 'run_unit', count(*) FROM run_unit
  UNION ALL SELECT 'run_goal', count(*) FROM run_goal
  UNION ALL SELECT 'issue', count(*) FROM issue;
"
unset PGPASSWORD
```

Сверьте количества с таблицей выше. Затем можно выполнить интеграционные тесты
с `KKD_INTEGRATION_TESTS=true ./mvnw clean verify`, запустить приложение
через `./mvnw spring-boot:run` и повторить curl-проверки.
Если локальная PostgreSQL слушает другой порт, измените его и в DB_URL, и в psql.

Полная матрица требований и результаты: [docs/verification.md](verification.md).
