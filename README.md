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

В `.env` обязательно задайте `JWT_SECRET`: это ключ подписи токенов, не короче 32 символов. Без него `docker compose` не запустится. Для своего окружения сгенерируйте новый ключ, например `openssl rand -base64 48`. Остальные переменные имеют значения по умолчанию (пользователь и пароль БД `vrazumi`), они годятся только для локальной разработки.

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html (кнопка **Authorize** принимает access-токен)
- OpenAPI-спецификация: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

Остановить: `docker compose down`. Начать с чистой базы: `docker compose down -v`.

## Бэкенд (core)

Код лежит в `backend/core/`.

### Стек

- JDK 25, Spring Boot 4.1 (Web MVC, Bean Validation, Data JPA, Security, OAuth2 Resource Server, Actuator)
- PostgreSQL 18, Liquibase (changelog в XML), Hibernate в режиме `validate`
- JWT (HS256), пароли в BCrypt
- MapStruct, Lombok
- springdoc-openapi 3 (Swagger UI)
- JUnit 5, Mockito, Testcontainers
- Docker, GitHub Actions, Spotless (google-java-format)

### Устройство

```
backend/core/src/main/java/ru/vrazumi/core
├── user/      # пользователи, регистрация
├── auth/      # вход, JWT, refresh-токены, настройка безопасности
├── deck/      # колоды
├── group/     # группы колод
├── card/      # карточки, типы карточек, проверка ответов
├── study/     # режим изучения
├── review/    # оценки ответов и автоматическая проверка (/answer)
└── common/    # ошибки, CORS-настройки, OpenAPI
```

Пакеты разложены по фичам: это будущие модули модульного монолита. Фича обращается к другой фиче только через её сервис.

### Локальный запуск (приложение из IDE)

```bash
docker compose up postgres
```

Затем запустите `ru.vrazumi.core.CoreApplication` из IDE (с переменной окружения `JWT_SECRET`) или из консоли:

```bash
cd backend/core && JWT_SECRET=local-dev-secret-at-least-32-characters ./mvnw spring-boot:run
```

По умолчанию приложение подключается к `localhost:5432/vrazumi` (пользователь и пароль `vrazumi`). Параметры меняются переменными окружения `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Срок жизни токенов: `JWT_ACCESS_TTL` (по умолчанию `15m`) и `JWT_REFRESH_TTL` (по умолчанию `30d`).

### Демо-данные

Пользователь **`demo` / `demo-password`** и его две колоды по 5 карточек. Это отдельные Liquibase-changeset'ы с `context="demo"`, по умолчанию они выключены.

- Docker: в `.env` задайте `LIQUIBASE_CONTEXTS=demo` и перезапустите `docker compose up --build`.
- IDE: переменная окружения `LIQUIBASE_CONTEXTS=demo` или свойство `spring.liquibase.contexts=demo`.

Changeset выполняется один раз. Если база уже создана без демо-данных, они добавятся при первом запуске с `demo`.

> **Миграция со спринта 1.** В спринте 2 у колод появился владелец. Колоды, созданные в спринте 1, удаляются вместе с карточками и оценками (кроме демо-колод: с контекстом `demo` их получает пользователь `demo`).

### Тесты и линтер

Нужны JDK 25 и запущенный Docker (для Testcontainers):

```bash
cd backend/core && ./mvnw spotless:check verify
```

На Windows вместо `./mvnw` используйте `mvnw.cmd`. Отформатировать код: `./mvnw spotless:apply`. Тестовый ключ JWT лежит в `src/test/resources/config/application.yml`, задавать `JWT_SECRET` для тестов не нужно.

- `*Test`: юнит-тесты сервисов (Mockito) и тесты веб-слоя (`@WebMvcTest` с настоящей конфигурацией безопасности), запускает Surefire.
- `*IT`: интеграционные тесты на PostgreSQL в Testcontainers (миграции, авторизация, изоляция пользователей, типы карточек, группы, демо-данные), запускает Failsafe на фазе `verify`.

### API

> **Для фронтенда.**
> - Базовый путь — **`/api`**. Формат JSON, даты в ISO-8601 UTC (`2026-10-01T09:30:00Z`).
> - **Все эндпоинты, кроме `POST /api/auth/register|login|refresh|logout`, требуют заголовок `Authorization: Bearer <accessToken>`.** Без токена или с истёкшим токеном — `401`.
> - Ошибки приходят в формате **`ProblemDetail`** (RFC 9457, `Content-Type: application/problem+json`).
> - CORS разрешён для `http://localhost:5173` и `http://localhost:3000`. Список задаётся переменной `CORS_ALLOWED_ORIGINS`.

#### Авторизация

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| POST | `/api/auth/register` | 201 | 400, 409 |
| POST | `/api/auth/login` | 200 | 400, 401 |
| POST | `/api/auth/refresh` | 200 | 400, 401 |
| POST | `/api/auth/logout` | 204 | 400 |
| GET | `/api/auth/me` | 200 | 401 |

- Регистрация: `username` — 3–32 символа (латиница, цифры, `_ . -`), уникален без учёта регистра. `email` — корректный адрес, хранится в нижнем регистре, уникален. `password` — от 8 символов и не больше 72 байт (72 латинских или 36 кириллических символов). Занятые имя или email — `409`.
- Вход: `{"login": "<username или email>", "password": "..."}`. Ответ: `accessToken` (JWT, живёт 15 минут), `tokenType: "Bearer"`, `expiresIn` в секундах, `refreshToken` (живёт 30 дней), `refreshExpiresIn`. Неверный логин или пароль — `401` с одинаковым сообщением.
- Обновление: `POST /api/auth/refresh {"refreshToken": "..."}` возвращает **новую пару**. Refresh-токен одноразовый: повторное использование старого токена отзывает все сессии пользователя.
- Выход: `POST /api/auth/logout {"refreshToken": "..."}` отзывает refresh-токен. Access-токен клиент просто удаляет.

#### Колоды, карточки, изучение, оценки, группы

Пользователь видит только свои колоды и группы. **Чужая колода, карточка или группа отвечает `404`**, как несуществующая.

| Метод | Путь | Успех | Ошибки |
|---|---|---|---|
| POST | `/api/decks` | 201 + `Location` | 400 |
| GET | `/api/decks` | 200 | — |
| GET | `/api/decks/{deckId}` | 200 | 404 |
| PUT | `/api/decks/{deckId}/group` | 200 | 404 |
| POST | `/api/decks/{deckId}/cards` | 201 + `Location` | 400, 404 |
| GET | `/api/decks/{deckId}/cards` | 200 | 404 |
| GET | `/api/decks/{deckId}/study` | 200 | 404 |
| POST | `/api/cards/{cardId}/review` | 201 | 400, 404 |
| POST | `/api/cards/{cardId}/answer` | 200 | 400, 404 |
| GET | `/api/groups` | 200 | — |
| POST | `/api/groups` | 201 + `Location` | 400, 409 |
| GET | `/api/groups/{groupId}` | 200 | 404 |
| PUT | `/api/groups/{groupId}` | 200 | 400, 404, 409 |
| DELETE | `/api/groups/{groupId}` | 204 | 404 |

Правила:

- `title` колоды: от 1 до 100 символов после обрезки пробелов. `description`: до 1000 символов, пустое значение сохраняется как `null`.
- `GET /api/decks`: новые колоды первыми, с полями `cardCount` и `groupId` (`null` — колода вне групп). Пустой список — `200 []`.
- `GET /api/decks/{id}/cards` и `/study`: карточки в порядке добавления. Пустая колода — `200`, а не ошибка.
- `Location` новой карточки указывает на `/api/cards/{cardId}`. Эндпоинта `GET` для одной карточки пока нет.

**Типы карточек** (`type` при создании, по умолчанию `NORMAL`):

| Тип | Что передать при создании | Как проверяется |
|---|---|---|
| `NORMAL` | `question`, `answer` (до 2000 символов) | Самооценка: `POST /api/cards/{id}/review {"rating": "KNOW"}` |
| `MULTIPLE_CHOICE` | `question`, `options: [{"text": "...", "correct": true}]` — 2–10 вариантов до 500 символов, хотя бы один верный, без повторов; `answer` не передаётся | `POST /api/cards/{id}/answer {"selectedOptionIds": [31, 33]}` |
| `TEXT_INPUT` | `question`, `answer` | `POST /api/cards/{id}/answer {"text": "went"}` — без учёта регистра и лишних пробелов |

- `/review` работает только для `NORMAL`. `rating`: одно из `DONT_KNOW`, `HARD`, `KNOW`, `EASY`. **Каждая оценка — новая запись**, повторная оценка ничего не перезаписывает.
- `/answer` проверяет ответ и сразу записывает оценку в ту же историю: `CORRECT → KNOW`, `PARTIAL → HARD` (выбрана часть верных или верные вместе с неверными), `INCORRECT → DONT_KNOW`. В ответе — `result`, правильный ответ (`correctOptionIds` или `correctAnswer`) и записанная оценка `review`. Пустой выбор, пустой текст или вариант не из этой карточки — `400`.
- `/study` не раскрывает ответ заранее: у `MULTIPLE_CHOICE` варианты приходят без поля `correct`, у `TEXT_INPUT` нет `answer`.

**Группы:** название — от 1 до 100 символов, уникально у пользователя без учёта регистра (`409`). В списке групп — `deckCount`. Перенос колоды: `PUT /api/decks/{id}/group {"groupId": 3}`, убрать из группы — `{"groupId": null}`. **Удаление группы не удаляет её колоды**: они остаются без группы (`groupId: null`).

#### Сценарий демо (curl)

```bash
# 1. Регистрация и вход
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username": "ivan", "email": "ivan@example.com", "password": "s3cret-pass"}'
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login": "ivan", "password": "s3cret-pass"}'
# {"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":900,"refreshToken":"...","refreshExpiresIn":2592000}
TOKEN=eyJ...   # accessToken из ответа

# 2. Колода и карточка с вариантами
curl -X POST http://localhost:8080/api/decks \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title": "Geography"}'
curl -X POST http://localhost:8080/api/decks/1/cards \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"type": "MULTIPLE_CHOICE", "question": "Capitals?", "options": [{"text": "Paris", "correct": true}, {"text": "Sydney"}, {"text": "Ottawa", "correct": true}]}'

# 3. Изучение: варианты без отметки верных
curl http://localhost:8080/api/decks/1/study -H "Authorization: Bearer $TOKEN"

# 4. Ответ: сервер проверяет и записывает оценку
curl -X POST http://localhost:8080/api/cards/1/answer \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"selectedOptionIds": [1, 3]}'
# {"result":"CORRECT","correctOptionIds":[1,3],"review":{"id":1,"cardId":1,"rating":"KNOW",...}}
```

> На Windows (cmd, PowerShell, Git Bash) curl может передать кириллицу из командной строки не в UTF-8, и сервер ответит 400 «не является корректным JSON». Сохраните тело в UTF-8 файл и отправьте его через `--data-binary @deck.json` или воспользуйтесь Swagger UI.

#### Формат ошибок

Ошибка валидации (400):

```json
{
  "title": "Некорректный запрос",
  "status": 400,
  "detail": "Запрос содержит ошибки валидации.",
  "instance": "/api/decks/1/cards",
  "errors": [
    { "field": "options", "message": "Отметьте хотя бы один верный вариант" },
    { "field": "options[1].text", "message": "Текст варианта обязателен" }
  ]
}
```

Нет токена или он истёк (401):

```json
{ "title": "Не авторизован", "status": 401, "detail": "Срок действия access-токена истёк. Обновите его.", "instance": "/api/decks" }
```

Занятое имя (409):

```json
{
  "title": "Конфликт",
  "status": 409,
  "detail": "Имя пользователя уже занято",
  "instance": "/api/auth/register",
  "errors": [{ "field": "username", "message": "Имя пользователя уже занято" }]
}
```

Не найдено (404):

```json
{ "title": "Не найдено", "status": 404, "detail": "Колода с id=42 не найдена", "instance": "/api/decks/42" }
```

Битый JSON, неизвестное значение enum (`rating`, `type`) и нечисловой id в пути (`/api/decks/abc`) тоже дают 400. Непредвиденные ошибки дают 500 без подробностей в ответе, стектрейс пишется в лог.

### CI

Workflow `Backend Core CI` (`.github/workflows/backend-core-ci.yml`) запускается на push и pull request, если изменилось что-то в `backend/core/`. Он выполняет `spotless:check` и `mvn verify` на JDK 25 (Temurin), включая тесты с Testcontainers.

## Как работаем с репозиторием

- Ветки: `feature/<номер-задачи>-<кратко>`, например `feature/42-deck-list`.
- В `main` изменения попадают только через Pull Request: зелёный CI и одобрение ревьюера.
- Владельцы частей репозитория указаны в `.github/CODEOWNERS`, они автоматически назначаются ревьюерами.
