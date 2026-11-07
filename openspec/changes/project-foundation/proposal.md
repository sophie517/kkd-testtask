# Proposal

## Why

KKD Service — Spring Boot skeleton без конфигурации базы данных, Liquibase и переменных окружения. Необходима базовая инфраструктура для запуска приложения с PostgreSQL, автоматического применения миграций.

## What Changes

- **Конфигурация PostgreSQL** — `application.yml` с `spring.datasource.*` и `spring.jpa.*` через переменные окружения (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- **Liquibase** — автоматический запуск миграций при старте Spring Boot; начальный changelog `db/changelog/master.yml`.
- **`.env.example`** — шаблон переменных окружения без реальных паролей.
- **Базовая структура пакетов** — `api/`, `application/`, `domain/`, `validation/`, `shared/` согласно `03-architecture.md`.

## Capabilities

### New Capabilities

- `project-infrastructure`: автоматические миграции Liquibase, конфигурация через переменные окружения.

### Modified Capabilities

- (нет — это первая инфраструктурная способность проекта)

## Impact

| Область | Влияние |
|---|---|
| `pom.xml` | Без изменений — все зависимости уже подключены |
| `src/main/resources/application.properties` | **Удаление** — заменяется на `application.yml` |
| `src/main/resources/application.yml` | **Создание** — datasource, jpa, liquibase конфигурация |
| `src/main/resources/db/changelog/` | **Создание** — начальный changelog |
| `.env.example` | **Создание** — шаблон переменных |
| `src/main/java/sophie517/testtask/kkd/` | **Создание** — пустые пакеты по архитектуре |
