# Spec Delta

## Purpose

Обеспечивает запуск KKD Service с PostgreSQL через конфигурацию из переменных окружения и автоматическое применение миграций Liquibase при старте.

> **Примечание:** Docker Compose и тестирование будут реализованы на финальном этапе разработки.

## ADDED Requirements

### Requirement: Application configuration via environment variables

The system SHALL use `application.yml` as the Spring Boot configuration file and read database connection parameters from environment variables `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Default values SHALL be used when variables are not set, to support local development. The old `application.properties` SHALL be removed.

#### Scenario: Application starts with environment variables set

- **WHEN** `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables are set
- **THEN** Spring Boot connects to PostgreSQL using the provided values

#### Scenario: Application starts with default values

- **WHEN** environment variables are not set
- **THEN** Spring Boot uses default values: `jdbc:postgresql://localhost:5432/kkd`, username `kkd`, password `kkd`

### Requirement: PostgreSQL healthcheck and startup dependency

The system SHALL verify PostgreSQL readiness before starting the KKD Service. The Docker Compose configuration SHALL include a healthcheck for the PostgreSQL container and the application container SHALL depend on the database being healthy.

#### Scenario: Docker Compose waits for PostgreSQL health

- **WHEN** `docker compose up` is executed
- **THEN** the `kkd-service` container starts only after the `db` container reports `service_healthy`

#### Scenario: PostgreSQL healthcheck verifies port

- **WHEN** the healthcheck runs
- **THEN** it executes `pg_isready -U kkd` against port 5432 with interval 5s and timeout 5s

### Requirement: Liquibase automatic migration on startup

The system SHALL apply Liquibase migrations automatically when the Spring Boot application starts. The changelog root SHALL be `db/changelog/master.yml`. Migrations SHALL be versioned and trackable via the `databasechangelog` table.

#### Scenario: Application applies migrations on startup

- **WHEN** the Spring Boot application starts with a fresh database
- **THEN** Liquibase applies all pending migrations from the YAML changelog

#### Scenario: Application skips migrations on second startup

- **WHEN** the Spring Boot application starts and all migrations are already applied
- **THEN** Liquibase does not execute any migration and the application starts normally

### Requirement: Package structure

The system SHALL organize Java source code into packages matching the architecture defined in `03-architecture.md`: `api/`, `application/`, `domain/`, `validation/`, `shared/`.

#### Scenario: Package directories exist

- **WHEN** the project is built
- **THEN** all package directories under `src/main/java/sophie517/testtask/kkd/` exist as defined in the architecture
