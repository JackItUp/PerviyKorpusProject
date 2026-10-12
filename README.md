# Вразуми — Web Anki

Веб-приложение для изучения карточек: колоды «вопрос — ответ», режим изучения и самооценка каждого ответа.

## Структура репозитория

```
/
├── backend/
│   ├── core/              # Java-ядро (Spring Boot)
│   └── analytics/         # Python-сервис аналитики — появится позже
├── frontend/              # фронтенд — появится позже
├── docs/                  # документация проекта
├── .github/
│   ├── workflows/         # CI: backend-core-ci.yml
│   └── CODEOWNERS
├── docker-compose.yml     # запуск всего проекта
├── .env.example
├── .gitattributes
├── .gitignore
└── README.md
```

## Быстрый запуск

Нужен Docker с Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

Без `.env` проект тоже запустится со значениями по умолчанию (пользователь и пароль БД `vrazumi`). Это годится только для локальной разработки.

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI-спецификация: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

Остановить: `docker compose down`. Начать с чистой базы: `docker compose down -v`.

## Бэкенд (core)

Код лежит в `backend/core/`.

### Стек

- JDK 25, Spring Boot 4.1 (Web MVC, Bean Validation, Data JPA, Actuator)
- PostgreSQL 18, Liquibase (changelog в XML), Hibernate в режиме `validate`
- MapStruct, Lombok
- springdoc-openapi 3 (Swagger UI)
- JUnit 5, Mockito, Testcontainers
- Docker, GitHub Actions, Spotless (google-java-format)

### Устройство

```
backend/core/src/main/java/ru/vrazumi/core
├── deck/      # колоды
├── card/      # карточки
├── study/     # режим изучения
├── review/    # оценки ответов
└── common/    # ошибки, CORS, OpenAPI
```

Пакеты разложены по фичам: это будущие модули модульного монолита. Фича обращается к другой фиче только через её сервис.

### Локальный запуск (приложение из IDE)

```bash
docker compose up postgres
```

Затем запустите `ru.vrazumi.core.CoreApplication` из IDE или из консоли:

```bash
cd backend/core && ./mvnw spring-boot:run
```

По умолчанию приложение подключается к `localhost:5432/vrazumi` (пользователь и пароль `vrazumi`). Параметры меняются переменными окружения `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

### Демо-данные

Две колоды по 5 карточек. Это отдельный Liquibase-changeset с `context="demo"`, по умолчанию он выключен.

- Docker: в `.env` задайте `LIQUIBASE_CONTEXTS=demo` и перезапустите `docker compose up --build`.
- IDE: переменная окружения `LIQUIBASE_CONTEXTS=demo` или свойство `spring.liquibase.contexts=demo`.

Changeset выполняется один раз. Если база уже создана без демо-данных, они добавятся при первом запуске с `demo`.

### Тесты и линтер

Нужны JDK 25 и запущенный Docker (для Testcontainers):

```bash
cd backend/core && ./mvnw spotless:check verify
```

На Windows вместо `./mvnw` используйте `mvnw.cmd`. Отформатировать код: `./mvnw spotless:apply`.

- `*Test`: юнит-тесты сервисов (Mockito) и тесты веб-слоя (`@WebMvcTest`), запускает Surefire.
- `*IT`: интеграционные тесты на PostgreSQL в Testcontainers (миграции, сквозной сценарий, демо-данные), запускает Failsafe на фазе `verify`.

### API

> **Для фронтенда.** Базовый путь — **`/api`**. Формат JSON, даты в ISO-8601 UTC (`2026-10-01T09:30:00Z`).
> Ошибки приходят в формате **`ProblemDetail`** (RFC 9457, `Content-Type: application/problem+json`).
> CORS разрешён для `http://localhost:5173` и `http://localhost:3000`. Список задаётся переменной `CORS_ALLOWED_ORIGINS`.

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| POST | `/api/decks` | 201 + `Location` | 400 |
| GET | `/api/decks` | 200 | — |
| GET | `/api/decks/{deckId}` | 200 | 404 |
| POST | `/api/decks/{deckId}/cards` | 201 + `Location` | 400, 404 |
| GET | `/api/decks/{deckId}/cards` | 200 | 404 |
| GET | `/api/decks/{deckId}/study` | 200 | 404 |
| POST | `/api/cards/{cardId}/review` | 201 | 400, 404 |

Правила:

- `title` колоды: от 1 до 100 символов после обрезки пробелов. `description`: до 1000 символов, пустое значение сохраняется как `null`.
- `question` и `answer` карточки: от 1 до 2000 символов после обрезки пробелов. Тип карточки всегда `NORMAL`.
- `rating`: одно из `DONT_KNOW`, `HARD`, `KNOW`, `EASY`. **Каждая оценка — новая запись**, повторная оценка ничего не перезаписывает.
- `GET /api/decks`: новые колоды первыми, с полем `cardCount`. Пустой список — `200 []`.
- `GET /api/decks/{id}/cards` и `/study`: карточки в порядке добавления. Пустая колода — `200`, а не ошибка.
- `Location` новой карточки указывает на `/api/cards/{cardId}`. Эндпоинта `GET` для одной карточки в этом спринте нет.

#### Сценарий демо (curl)

```bash
# 1. Создать колоду
curl -i -X POST http://localhost:8080/api/decks \
  -H 'Content-Type: application/json' \
  -d '{"title": "Английские глаголы", "description": "Неправильные глаголы"}'
# HTTP/1.1 201, Location: http://localhost:8080/api/decks/1
# {"id":1,"title":"Английские глаголы","description":"Неправильные глаголы","cardCount":0,...}

# 2. Добавить карточку
curl -i -X POST http://localhost:8080/api/decks/1/cards \
  -H 'Content-Type: application/json' \
  -d '{"question": "go", "answer": "went, gone"}'
# {"id":1,"deckId":1,"type":"NORMAL","question":"go","answer":"went, gone",...}

# 3. Начать изучение
curl http://localhost:8080/api/decks/1/study
# {"deckId":1,"deckTitle":"Английские глаголы","cards":[{"id":1,"type":"NORMAL","question":"go","answer":"went, gone"}]}

# 4. Оценить ответ
curl -i -X POST http://localhost:8080/api/cards/1/review \
  -H 'Content-Type: application/json' \
  -d '{"rating": "KNOW"}'
# HTTP/1.1 201
# {"id":1,"cardId":1,"rating":"KNOW","reviewedAt":"2026-10-12T10:00:00.123456Z"}

# Список колод
curl http://localhost:8080/api/decks
```

> На Windows (cmd, PowerShell, Git Bash) curl может передать кириллицу из командной строки не в UTF-8, и сервер ответит 400 «не является корректным JSON». Сохраните тело в UTF-8 файл и отправьте его через `--data-binary @deck.json` или воспользуйтесь Swagger UI.

#### Формат ошибок

Ошибка валидации (400):

```json
{
  "title": "Некорректный запрос",
  "status": 400,
  "detail": "Запрос содержит ошибки валидации.",
  "instance": "/api/decks",
  "errors": [
    { "field": "title", "message": "Название колоды обязательно" }
  ]
}
```

Недопустимое значение оценки (400):

```json
{
  "title": "Некорректный запрос",
  "status": 400,
  "detail": "Недопустимое значение поля 'rating'.",
  "instance": "/api/cards/1/review",
  "errors": [
    { "field": "rating", "message": "Недопустимое значение 'SUPER'. Допустимые значения: DONT_KNOW, HARD, KNOW, EASY" }
  ]
}
```

Не найдено (404):

```json
{ "title": "Не найдено", "status": 404, "detail": "Колода с id=42 не найдена", "instance": "/api/decks/42" }
```

Битый JSON и нечисловой id в пути (`/api/decks/abc`) тоже дают 400. Непредвиденные ошибки дают 500 без подробностей в ответе, стектрейс пишется в лог.

### CI

Workflow `Backend Core CI` (`.github/workflows/backend-core-ci.yml`) запускается на push и pull request, если изменилось что-то в `backend/core/`. Он выполняет `spotless:check` и `mvn verify` на JDK 25 (Temurin), включая тесты с Testcontainers.

## Как работаем с репозиторием

- Ветки: `feature/<номер-задачи>-<кратко>`, например `feature/42-deck-list`.
- В `main` изменения попадают только через Pull Request: зелёный CI и одобрение ревьюера.
- Владельцы частей репозитория указаны в `.github/CODEOWNERS`, они автоматически назначаются ревьюерами.
