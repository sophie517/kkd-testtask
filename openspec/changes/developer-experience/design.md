# Design

## Decisions

Сохранить Dockerfile и Compose. Lombok @Getter/@AllArgsConstructor/@NoArgsConstructor
в сущностях сохраняет публичные getters и protected no-arg конструкторы.
Ручные equals/hashCode составных ID остаются; @Data и setters не добавляются.
@RequiredArgsConstructor заменяет конструкторы внедрения зависимостей.

springdoc-openapi-starter-webmvc-ui 2.8.17 совместим с Spring Boot 3.5.
Параметры MultiValueMap описать явно аннотациями; техническую Map скрыть.
Swagger UI и JSON проверяются MockMvc с настоящим Spring-контекстом и mock-репозиториями.

README оставляет по одному основному пути запуска и тестирования. Подробные ручные
операции миграций выносятся в docs/migrations.md.
