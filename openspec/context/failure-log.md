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
