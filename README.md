# Система прокторинга (SPK)

## Описание
Это серверный проект для управления удалёнными экзаменами и контроля академической честности. Внутри он содержит:

- REST API на Spring Boot
- Доменные сущности для пользователей, экзаменационных сессий и нарушений
- Хранение данных в PostgreSQL
- Асинхронную обработку событий через Kafka
- Поддержку Redis для кэширования и ограничения частоты запросов
- Интеграцию со службой AI для детекции нарушений

Приложение не просто «сайт с базой данных» — это сервис, который управляет логикой проведения экзамена, верификацией студентов и фиксацией нарушений.

## Почему здесь Docker, Redis и Gradle

### Docker
Docker позволяет запускать всю систему как набор контейнеров. Это удобно, потому что проект зависит не только от Java, но и от внешних сервисов:
- PostgreSQL — основная база данных
- Redis — кэш и rate limiting
- Kafka — асинхронная очередь для AI-обработки

Docker Compose помогает поднимать все эти сервисы вместе, без ручной настройки каждой зависимости.

### Redis
Redis используется не как основная база, а для ускорения и надежности:
- кэширование
- ограничение частоты запросов
- временное хранение состояния

### Gradle
Gradle управляет сборкой Java-проекта:
- компиляция модулей
- управление зависимостями
- запуск тестов
- сборка артефактов

Без Gradle в Java-проекте собирать и запускать сервис было бы неудобно.

## Что делает приложение

### Основные сценарии
- Проктор создаёт экзаменационную сессию
- Студент стартует экзамен по токену
- Система фиксирует нарушения в рамках сессии
- Нарушения привязаны к конкретному студенту
- Администратор мониторит систему и управляет пользователями

### Новое в проекте
- Поддержка групповых сессий с несколькими студентами
- Привязка нарушений к studentId внутри sessionId
- Обновлённый интерфейс для создания групповых экзаменов и отправки нарушений

## Структура проекта

- `proctoring-application/` — бизнес-логика и API
- `proctoring-domain/` — доменные сущности и правила
- `proctoring-infrastructure/` — репозитории и интеграции
- `proctoring-interfaces/` — REST-контроллеры и frontend
- `docker-compose.yml` — окружение для локального запуска
- `k8s/` — Kubernetes-манифесты

## Быстрый запуск

1. Скопируйте шаблон окружения:
```bash
cp .env.example .env
```
2. Отредактируйте `.env` и укажите OPENAI_API_KEY и другие параметры.
3. Запустите контейнеры:
```bash
docker compose up --build
```
4. Проверьте статус:
```bash
curl http://localhost:8080/actuator/health
```

## Запуск без Docker

Если нужно запустить только бэкенд локально:
```bash
./gradlew clean bootRun
```

## Примеры API

### Регистрация
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"student@aiu.kz","username":"student1","password":"pass123","firstName":"Ershat","lastName":"Toleubaev","role":"STUDENT"}'
```

### Вход
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"pass123"}'
```

### Создание групповой сессии
```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"studentIds":["<STUDENT_ID_1>","<STUDENT_ID_2>"],"groupName":"Group A","disciplineCode":"CS101","disciplineName":"Remote Exam","scheduledStart":"2026-05-08T10:00:00","scheduledEnd":"2026-05-08T12:00:00","maxViolations":5,"violationThreshold":0.7}'
```

### Отправка нарушения для конкретного студента
```bash
curl -X POST "http://localhost:8080/api/violations?sessionId=<SESSION_ID>&studentId=<STUDENT_ID>&type=FACE_NOT_VISIBLE&confidence=0.82&frameTimestamp=1234567890&description=Manual+test" \
  -H "Authorization: Bearer <TOKEN>"
```

## Переменные окружения

Пример `.env`:
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/proctoring_db
SPRING_DATASOURCE_USERNAME=proctoring_user
SPRING_DATASOURCE_PASSWORD=proctoring_pass

JWT_SECRET=your_jwt_secret_minimum_256_bits_base64
APP_JWT_EXPIRATION=86400000
APP_JWT_REFRESH_EXPIRATION=604800000

OPENAI_API_KEY=sk-...

GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret

AI_PROVIDER=openai
AI_OPENAI_MODEL=gpt-4o-mini
AI_TIMEOUT=30s
AI_MAX_RETRIES=3

RATE_LIMIT_AI_REQUESTS=60
RATE_LIMIT_AI_WINDOW=60

SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

CORS_ALLOWED_ORIGINS=*
```

## Тестирование

### Unit тесты
```bash
./gradlew test
```

### Интеграционные тесты
```bash
./gradlew integrationTest
```

## Что важно знать

- Это полноценный сервис, а не просто статический сайт.
- Redis используется для ускорения и ограничения частоты запросов.
- Docker упрощает запуск всей инфраструктуры.
- Gradle управляет сборкой и тестами.

## Участники
- **Toleubayev Ershat** — разработчик

© 2026 AIU. Все права защищены.
