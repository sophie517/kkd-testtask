# Proposal

## Why

Пользователь отменил удаление Docker: требование Compose сохраняется. README
перегружен повторяющимися инструкциями; нужны Lombok и Swagger для удобства разработки.

## Scope

Сократить README, вынести ручные миграции в docs/migrations.md, заменить шаблонные
геттеры/конструкторы Lombok и добавить Swagger UI/OpenAPI для трёх GET endpoint.

## Non-goals

Изменения бизнес-правил, миграций, API JSON, отказ от Docker, новые управляющие endpoint.

## Capabilities

### Modified Capabilities

- `kkd-api`: добавить интерактивное описание текущего API.

## Impact

pom.xml, JPA-модель, конструкторы сервисов, KkdController, тесты и документация.
Lombok — только обработка аннотаций; springdoc — документация API.
