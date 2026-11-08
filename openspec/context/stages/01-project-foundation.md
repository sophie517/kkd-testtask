# Этап 1: Инфраструктура приложения

Статус: реализация и финальная приёмка завершены 2026-11-08.
Проверки PostgreSQL/Docker/HTTP и rollback выполнены; [отчёт](../../../docs/verification.md).
OpenSpec change: [project-foundation](../../changes/archive/2026-11-08-project-foundation/proposal.md).
Порядок и границы: [общая карта](../08-delivery-stages.md).

## Основание и обязательное чтение

[Задание](../_raw/business-assignment.md) задаёт обязательный результат продукта.
[Общий контекст](../00-start-here.md) задаёт выбранные уточнения.
Дополнительно прочитать:
- [04-engineering-standards.md](../04-engineering-standards.md)
- [05-security-and-compliance.md](../05-security-and-compliance.md)
- [06-testing-and-quality.md](../06-testing-and-quality.md)

Зависимости реализации: Нет.
Достаточно текущего skeleton. Старая схема БД не мигрируется: используем отдельную пустую БД и новый volume.

## Результат этапа

Приложение собирается и запускается вместе с готовой PostgreSQL; Liquibase читает пустой changelog, доменных таблиц ещё нет.

## Входит в этап

- Согласовать существующие Maven Wrapper, Java 21, Spring Boot 3 и настройки приложения без бизнес-классов.
- Добавить Dockerfile, .dockerignore и Compose с приложением, PostgreSQL 15+, healthcheck и отдельным volume.
- Создать пустой корневой changelog, включить Liquibase и проверить подключение к БД.
- Описать окружение и запуск инфраструктуры в начальном README.

## Не входит

- Бизнес-таблицы, JPA-сущности, справочник правил и демонстрационные цели.
- Движок правил, запуск проверок и REST API.
- Новые зависимости, мониторинг и авторизация.

## Передача следующему этапу

Результат принимается только после проверок [tasks.md](../../changes/archive/2026-11-08-project-foundation/tasks.md).
Чекбоксы отражают фактически пройденные проверки. Наличие файлов не заменяет проверку запуска.
Перед реализацией перечитать change и принятые результаты предыдущего этапа:
если они изменили контракт, сначала согласовать правку связанных планов.

Поведенческие требования: [project-infrastructure](../../changes/archive/2026-11-08-project-foundation/specs/project-infrastructure/spec.md).
Подход и альтернативы: [design.md](../../changes/archive/2026-11-08-project-foundation/design.md).
