---
owner: sophie517
updated: 2026-11-04
---

# 04 — Engineering Standards

## Технологический стек

| Компонент | Технология | Версия |
|---|---|---|
| Язык | Java | 21 |
| Фреймворк | Spring Boot | 3.5.16 |
| ORM | Spring Data JPA | — |
| База данных | PostgreSQL | 15+ |
| Миграции | Liquibase | — |
| Сборка | Maven | — |
| Тесты | JUnit 5 | — |
| Логирование | SLF4J + Logback | — |
| Код-генерация | Lombok | — |

## Стиль кода

- Ориентация на **Google Java Style Guide**.
- Форматирование средствами **IntelliJ IDEA**.
- Checkstyle и Spotless **не подключены**.

## Зависимости

- **Минимум зависимостей.** Не добавлять без согласования.
- Lombok используется для сокращения boilerplate.
- Bean Validation (`jakarta.validation`) **не используется**.

## Сборка и запуск

```bash
# Сборка
./mvnw clean package

# Запуск тестов
./mvnw test

# Запуск приложения
./mvnw spring-boot:run

# Docker Compose — полный стек
docker compose up
```

## Docker Compose

- `docker-compose.yml` **обязателен**.
- Запускает PostgreSQL и KKD Service.
- Liquibase применяется автоматически при старте Spring Boot.
- Используется `healthcheck` PostgreSQL с зависимостью `service_healthy`.

## Переменные окружения

Секреты и настройки подключения выносятся в переменные окружения.

| Переменная | Описание | Пример |
|---|---|---|
| `DB_URL` | URL подключения к PostgreSQL | `jdbc:postgresql://db:5432/kkd` |
| `DB_USERNAME` | Имя пользователя БД | `kkd` |
| `DB_PASSWORD` | Пароль БД | `***` |

Файл `.env.example` без реальных паролей — обязателен.

## Логирование

- Уровни: **INFO**, **WARN**, **ERROR**.
- Формат: обычный читаемый текст (не JSON).
- **Не логируем:** ФИО, email, фотографии, Base64, пароли, токены, полные тела HTTP-запросов/ответов.
- **Не логируем** `owner_id` без необходимости.
- Для диагностики: `run_id`, коды правил, количество нарушений, технические ошибки.
