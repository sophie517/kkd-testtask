---
owner: sophie517
updated: 2026-11-04
---

# 03 — Architecture

## Архитектурные инварианты

1. **Validation Engine — чистый компонент.** Не зависит от Spring, JPA и базы данных. Получает подготовленные данные через `ValidationContext` и возвращает список найденных нарушений.
2. **Контроллеры не обращаются к репозиториям напрямую.** Путь: Controller → Service → Repository.
3. **JPA Entity и API DTO разделены.** Entity не используются напрямую в API.
4. **Сравнение прогонов — в Service Layer,** а не в Validation Engine.
5. **Правила проверяются независимо от Spring-контекста.** Каждый Rule — чистый Java-класс.

## Структура пакетов

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

## Слои

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

## Архитектурные решения (ADR)

| ADR | Тема |
|---|---|
| [ADR/001-validation-engine-separation.md](./ADR/001-validation-engine-separation.md) | Отделение Validation Engine от Spring-зависимостей |

## Принятые решения

| Решение | Описание |
|---|---|
| Каталог правил: метаданные в БД, логика в Java | Таблица `kkd_rule` (Liquibase) хранит `code`, `description`, `severity` для справочного доступа. Исполняемая логика — 4 чистых Java-класса в `validation/impl/`. Никакого смешивания. |
