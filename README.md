# Учебная платформа

## Запуск в Docker

Из корня проекта выполните две команды. Потребуются Docker и Docker Compose.

```sh
cp -n .env.example .env
docker compose --profile tools up --build -d --wait
```

Compose соберёт приложение на Java 21 и поднимет PostgreSQL 17. Профиль `tools` также запустит Adminer. При следующих запусках достаточно второй команды.

- [Swagger UI](http://localhost:8080/swagger-ui.html) — документация и вызов API.
- [Состояние приложения](http://localhost:8080/actuator/health) — при готовности возвращает `UP`.
- [Adminer](http://localhost:8081) — выберите PostgreSQL, сервер `db`, базу и учётные данные из `.env`.

Для остановки выполните `docker compose --profile tools down`. Данные PostgreSQL сохраняются в томе `pgdata`.
