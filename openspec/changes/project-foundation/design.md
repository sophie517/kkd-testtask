# Design

## Context

KKD Service — Spring Boot skeleton с подключёнными `spring-boot-starter-data-jpa`, `liquibase-core`, `spring-boot-starter-web` и `postgresql`. `application.properties` содержит только `spring.application.name`. Тест `KkdServiceApplicationTests` исключает `DataSourceAutoConfiguration` и отключает Liquibase. Docker Compose, `.env.example` и пакетная структура отсутствуют.

## Goals / Non-Goals

**Goals:**
- Настроить подключение к PostgreSQL через переменные окружения
- Настроить Liquibase с автоматическим применением миграций (YAML формат)
- Создать базовую пакетную структуру по `03-architecture.md`
- Обновить тест так, чтобы он проверял запуск приложения без Testcontainers

**Non-Goals:**
- Создание JPA Entity, Repository, Service, Controller
- Реализация бизнес-логики правил ККД
- Создание API endpoints
- Настройка мониторинга, логирования (кроме базового SLF4J)
- Настройка CI/CD

## Decisions

### 1. Конфигурация БД: application.yml с переменными окружения

**Решение:** Использовать `application.yml` с Spring Boot placeholder-синтаксисом `${DB_URL:jdbc:postgresql://localhost:5432/kkd}` для подстановки переменных окружения с дефолтными значениями. Старый `application.properties` удаляется.

**Альтернативы:**
- `application.properties` — проект уже использует `.properties`, но YAML лучше структурирует вложенные настройки (datasource, jpa, liquibase)
- `application-{profile}.properties` — избыточно для v1, один профиль

**Обоснование:** YAML удобнее для вложенных Spring Boot настроек (datasource.url, jpa.hibernate.ddl-auto, liquibase.change-log). Spring Boot автоматически подставляет переменные окружения из `.env` файла (docker-compose) и хост-системы. Дефолтные значения позволяют запускать локально без `.env`.

### 2. Docker Compose: healthcheck PostgreSQL + depends_on

**Решение:** Использовать `pg_isready` для healthcheck PostgreSQL и `depends_on: db` с условием `service_healthy`.

**Альтернативы:**
- `wait-for-it.sh` — дополнительная зависимость, не нужна с `service_healthy`
- `spring.jpa.hibernate.ddl-auto=create` — не соответствует требованию Liquibase

**Обоснование:** Встроенный healthcheck PostgreSQL + `service_healthy` — нативный подход Docker Compose, без дополнительных скриптов.

### 3. Liquibase: changelog root `db/changelog/master.yml`

**Решение:** Использовать YAML-формат changelog с `changeSet` для начальной структуры. Путь: `src/main/resources/db/changelog/master.yml`.

**Альтернативы:**
- XML changelog — стандартный, но YAML читаемее и компактнее
- JSON changelog — менее читабелен для миграций

**Обоснование:** YAML — читаемый формат для Liquibase миграций. Spring Boot поддерживает YAML changelog из коробки (достаточно указать путь в `spring.liquibase.change-log`).

### 4. Пакетная структура: пустые пакеты на этом этапе

**Решение:** Создать пустые директории пакетов (`api/controller`, `api/dto`, `application/service`, `domain/model`, `domain/repository`, `validation/impl`) без кода.

**Альтернативы:**
- Создать пакеты с заглушками (placeholder classes) — избыточно
- Создать пакеты позже — нарушит `03-architecture.md`

**Обоснование:** Пакеты определены в архитектуре. Пустые директории не влияют на сборку и позволяют следующей фазе писать код в нужные места.

### 5. Тест: contextLoads без Testcontainers

**Решение:** Базовый `@SpringBootTest` без Testcontainers. Тест проверяет, что контекст Spring Boot загружается. Полноценное тестирование с PostgreSQL будет реализовано на финальном этапе.

**Альтернативы:**
- Testcontainers для PostgreSQL — отложено на финальный этап
- H2 in-memory DB — не соответствует требованию PostgreSQL

**Обоснование:** На этом этапе достаточно проверить, что конфигурация Spring Boot валидна. Полноценные тесты с БД будут на финальном этапе.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| YAML changelog может отличаться от привычного XML | Spring Boot поддерживает YAML changelog из коробки; миграция выполнена для master.yml |
| Тест не проверяет реальную БД | Тест проверяет загрузку контекста; полноценные тесты с PostgreSQL будут на финальном этапе |
| Docker Compose не реализован | Будет реализован на финальном этапе разработки |
| Пустые пакеты — визуальный шум | Пакеты не влияют на сборку; будут заполнены в следующих changes |

## Migration Plan

1. Создать/изменить файлы конфигурации (YAML changelog, application.yml)
2. Создать пустые пакеты
3. Обновить тест
4. Запустить `./mvnw clean package` — убедиться, что сборка проходит

> **Отложено на финальный этап:** Docker Compose, тестирование с Testcontainers, `.env.example`.

**Rollback:** Откат к предыдущему коммиту Git — все изменения в новых/изменённых файлах.

## Open Questions

Нет. Все технические решения определены на основе согласованных контекстных файлов.
