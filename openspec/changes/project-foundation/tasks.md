# Tasks

## 1. Конфигурация PostgreSQL и Liquibase

- [x] 1.1 Создать `src/main/resources/application.yml` — datasource, jpa/hibernate, liquibase с переменными окружения и дефолтными значениями; удалить `src/main/resources/application.properties`, затем проверить что `./mvnw clean compile` проходит
- [x] 1.2 Создать `src/main/resources/db/changelog/master.yml` — пустой changelog с `databaseChangeLog` root element, затем проверить что Spring Boot загружает changelog без ошибок при старте

## 2. Docker Compose

> **Отложено на финальный этап разработки.** Конфигурация Docker Compose, `.env.example` и связанные артефакты будут реализованы позже.

- [ ] 2.1 Создать `docker-compose.yml` — сервисы `db` (PostgreSQL 15, переменные из env, healthcheck `pg_isready`, named volume `kkd-db-data`) и `kkd-service` (build из `.`, depends_on `db` с `service_healthy`, порт 8080, переменные окружения из `.env`)
- [ ] 2.2 Создать `.env.example` — `DB_URL=jdbc:postgresql://db:5432/kkd`, `DB_USERNAME=kkd`, `DB_PASSWORD=kkd`

## 3. Пакетная структура

- [x] 3.1 Создать директории пакетов по `03-architecture.md`: `api/controller`, `api/dto`, `api/exception`, `application/service`, `domain/model`, `domain/repository`, `validation/impl`, `shared`, затем проверить что все директории существуют

## 4. Обновление теста

- [x] 4.1 Обновить `src/test/java/sophie517/testtask/kkd/KkdServiceApplicationTests.java` — базовый тест `contextLoads` без Testcontainers (полноценное тестирование будет на финальном этапе)

## 5. Финальная проверка

- [x] 5.1 Запустить `./mvnw clean package` — убедиться, что сборка завершается успешно без ошибок компиляции
- [ ] 5.2 Запустить `docker compose up` — **Отложено на финальный этап разработки**.
