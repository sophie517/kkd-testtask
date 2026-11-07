---
owner: sophie517
updated: 2026-11-04
---

# Failure Log — Журнал ошибок и изменений контекста

## 2026-11-04

### Инициализация
- Запущено интервью для формирования контекста KKD Service.
- Проект на начальной стадии: только Spring Boot skeleton.
- Все контекстные файлы созданы по результатам интервью.

### Созданные файлы
- `00-start-here.md` — навигация, главные запреты
- `01-product-context.md` — назначение, пользователи, сценарии, инварианты
- `02-domain-glossary.md` — термины, правила, бизнес-правила
- `03-architecture.md` — архитектурные инварианты, структура пакетов
- `04-engineering-standards.md` — стек, инструменты, Docker Compose
- `05-security-and-compliance.md` — классификация данных, безопасность
- `06-testing-and-quality.md` — стратегия тестирования, приёмочные критерии
- `07-anti-patterns.md` — запрещённые паттерны и предпочтительные альтернативы
- `ADR/001-validation-engine-separation.md` — отделение Validation Engine от Spring
- `examples/README.md` — примеры ответов API и структуры кода
- `failure-log.md` — этот файл

### TODO
- [ ] Определить стратегию хранения каталога правил (БД vs код) — см. 03-architecture.md, 05-security-and-compliance.md.
- [ ] **Отложено на финальный этап:** Docker Compose, Testcontainers, `.env.example`

## 2026-11-06

### Финальная сверка с бизнес-заданием

**Цель:** сверить все контекстные файлы с `business-assignment.md`, исправить расхождения, зафиксировать решения.

### Исправления

1. **`06-testing-and-quality.md`** — добавлена секция «Тестирование первого прогона»:
   - Проверка `null` для `compared_to`, `previous`, `delta`, `trend` в первом прогоне
   - Проверка что все проблемы первого прогона — `new`, нет `persisting`/`resolved`

2. **`02-domain-glossary.md`** — убран TBD из описания Rule:
   - Зафиксировано решение: метаданные в БД (`kkd_rule`), логика в Java
   - Добавлена секция «Решения v1» с пометкой всех архитектурных решений

3. **`03-architecture.md`** — убран TBD из секции «Хранение каталога правил»:
   - Заменено на секцию «Принятые решения» с описанием архитектуры каталога

4. **`05-security-and-compliance.md`** — убран TODO по каталогу правил:
   - Заменено на секцию «Принятые решения»

5. **`01-product-context.md`** — Non-goal помечен явно как решение v1

### Подтверждённые соответствия

- ✅ HTTP-статусы 404/422 и единый формат ошибок согласованы в бизнес-задании, `06-testing-and-quality.md` и `01-product-context.md`
- ✅ Unit-тесты всех 4 правил покрыты в `06-testing-and-quality.md`
- ✅ Граничные случаи проверены для всех 4 правил (требование — минимум 2)
- ✅ Тесты дельты: 3 сценария (new/persisting/resolved)
- ✅ Архитектура каталога правил: метаданные в БД + логика в Java

### Оставшиеся TODO

| № | TODO | Файл |
|---|---|---|
| 1 | Реализация каталога правил: создать Liquibase-миграцию для `kkd_rule` | — |
| 2 | Seed-данные: 20-30 целей в 2-3 подразделениях, 2 прогона с нарушениями каждого правила | `01-product-context.md` |
| 3 | DTO-классы для GET /api/kkd/summary (метрики с дельтой и трендом) | `03-architecture.md` |
| 4 | DTO-классы для GET /api/kkd/issues (проблемы с фильтрами и пагинацией) | `03-architecture.md` |

## 2026-11-07

### Конвертация миграций и удаление отложенных артефактов

**Цель:** перевести миграции в YAML формат, убрать Docker Compose и Testcontainers — перенести на финальный этап.

### Изменения

1. **Миграции:** `master.xml` → `master.yml` (YAML формат Liquibase)
2. **application.yml:** обновлён путь changelog на `master.yml`
3. **pom.xml:** удалены зависимости `testcontainers:junit-jupiter` и `testcontainers:postgresql`
4. **KkdServiceApplicationTests.java:** упрощён — убран Testcontainers
5. **docker-compose.yml:** удалён
6. **04-engineering-standards.md:** удалены секции Docker Compose, перенос на финальный этап
7. **06-testing-and-quality.md:** приёмочные критерии помечены как `TODO`, тестирование отложено
8. **project-foundation/tasks.md:** задачи Docker и тестов помечены как отложенные
9. **project-foundation/design.md:** обновлены решения, риски и план миграции
10. **project-foundation/spec.md:** удалены требования Docker Compose
11. **config.yaml:** удалён Docker Compose из описания стека
