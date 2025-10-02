# Bank Card Management Service

## Краткое описание

Сервис позволяет создавать и управлять банковскими картами, просматривать карты, выполнять переводы между собственными
картами, управлять пользователями и ролями.
Проект использует Spring Boot, Spring Security (JWT), Spring Data JPA, Liquibase, PostgreSQL/MySQL и Docker Compose.

---

### Требования

* Java 17 или новее
* Maven 3.8+
* Docker и Docker Compose (для запуска dev-окружения)
* Git

---

### Переменные окружения

Файл `.env.example` в корне проекта:

```env
# Database
POSTGRES_DB=bank
POSTGRES_USER=bankuser
POSTGRES_PASSWORD=changeme
POSTGRES_HOST_PORT=5432

# App
DB_HOST=db
DB_PORT=5432
DB_NAME=bank
DB_USER=bankuser
DB_PASSWORD=changeme

# App secrets
JWT_SECRET=change-me-very-secret
JWT_EXP_MS=3600000
CIPHER_KEY_BASE64=93nF3d+k2H/51YGbAS38HSlFhSys6Tdn4gJhZUelE64=
APP_HOST_PORT=8080
```

---

### Запуск через Docker Compose
1. Перенести содержимаое файла .env.example в реальный созданный .env в корне проекта

2. Собрать образ приложения и запустить контейнеры:

docker-compose up --build -d

3. Остановить и удалить контейнеры:

docker-compose down

---

## Миграции базы данных

Проект использует Liquibase. Скрипт расположены в `src/main/resources/db/migration`

При запуске приложения Liquibase автоматически применит миграции к базе данных.

---

## Документация API

* OpenAPI / Swagger UI доступен по адресу: `http://localhost:8080/swagger-ui/index.html`.
* Файл спецификации: `docs/openapi.yaml`в ресурсах проекта

---

## Как пользоваться API — коротко

1. Зарегистрируйте пользователя (учетная запись администратора создается автоматически, логин/пароль - admin/admin).
2. Войдите и получите JWT (`/auth/login`).
3. Авторизируйтесь используя JWT и выполняйте запросы.
4. Примеры:

    * Создать карту (ADMIN): `POST /api/admin/cards`
    * Просмотреть свои карты (USER): `GET /api/cards`
    * Перевести между своими картами: `POST /api/cards/transfer`

---

## Список конечных точек

* `POST /api/auth/login` — получение JWT
* `GET /api/cards` — получить карты пользователя (пагинация + фильтрация)
* `GET /api/cards/{id}` — информация о карте

USER:

* `POST /api/cards/transfer` — перевод между своими картами
* `POST /api/cards/{id}/request-block` — запрос на блокировку карты (mock)

ADMIN:

* `POST /api/auth/register` — регистрация пользователя
* `POST /api/cards` — создать карту
* `PATCH /api/cards/{id}/block` — блокировать карту
* `PUT /ap/cards/{id}` - обновить карту
* `DELETE /ap/cards/{id}` — удалить карту

---

## Безопасность и соответствие требованиям

* Маскирование номера карты при выводе
* Шифрование номера карты и других чувствительных полей в БД
* Ролевой доступ (ADMIN / USER)
* Валидация входных данных и централизованная обработка ошибок
