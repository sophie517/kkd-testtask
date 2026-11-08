# Spec Delta

## ADDED Requirements

### Requirement: Интерактивное описание API

Сервис SHALL предоставлять OpenAPI JSON и Swagger UI для трёх GET endpoint с параметрами фильтров и пагинации.

#### Scenario: Просмотр документации
- **WHEN** разработчик открывает /swagger-ui/index.html
- **THEN** доступен интерфейс выполнения запросов

#### Scenario: Спецификация параметров
- **WHEN** запрошен /v3/api-docs
- **THEN** ответ содержит три GET endpoint, обязательные run_id/unit_id сводки и фильтры/пагинацию списка проблем
