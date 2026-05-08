# Система прокторинга (SPK) — Бэкенд API

## Содержание
- [Обзор](#обзор)
- [Архитектура](#архитектура)
- [Технологический стек](#технологический-стек)
- [Возможности](#возможности)
- [Установка и настройка](#установка-и-настройка)
- [Документация API](#документация-api)
- [Переменные окружения](#переменные-окружения)
- [Тестирование](#тестирование)
- [Безопасность](#безопасность)
- [Соответствие GDPR](#соответствие-gdpr)
- [Деплой](#деплой)
- [Производительность](#производительность)
- [Мониторинг и наблюдаемость](#мониторинг-и-наблюдаемость)
- [Будущие улучшения](#будущие-улучшения)
- [Участники](#участники)

## Обзор
Система прокторинга (SPK) — это бэкенд-сервис для контроля академической честности во время удалённых экзаменов. Система использует AI-анализ через OpenAI Vision API для обнаружения нарушений (несколько лиц, использование телефона, отвод взгляда и др.) и предоставляет ролевой доступ для студентов, прокторов и администраторов.

Основные возможности:
- Безопасная аутентификация JWT с поддержкой OAuth2 (Google)
- Реальное время violation detection (асинхронно через Kafka)
- Аудит-логирование и функции соответствия GDPR
- Ограничение частоты запросов и кэширование в Redis

## Архитектура

### Гексагональная/Чистая архитектура
```
┌─────────────────────────────────────────────┐
│          Слой интерфейсов (REST)            │
│  AuthController, SessionController, и т.д.  │
├─────────────────────────────────────────────┤
│         Слой приложения (Сервисы)           │
│ UserService, ExamSessionService, AIService  │
├─────────────────────────────────────────────┤
│        Доменный слой (Сущности)             │
│    User, ExamSession, Violation, AuditLog   │
├─────────────────────────────────────────────┤
│        Инфраструктурный слой                │
│ Repositories, Kafka, Redis, OpenAI Client  │
└─────────────────────────────────────────────┘
```

### Поток данных (AI-анализ)
1. **Студент начинает сессию** → Статус сессии становится ACTIVE
2. **Видеопоток фреймов** → Отправляется в Kafka топик `ai-processing-requests`
3. **AI Worker обрабатывает** → Вызывает OpenAI Vision API
4. **Результаты обрабатываются** → Violations сохраняются, уведомления отправляются
5. **Панель проктора** → Реальное время обновлений через WebSocket (в планах)

## Технологический стек
- **Java 17** & **Spring Boot 3.3.4**
- **PostgreSQL 15** (основное хранение)
- **Redis 7** (кэширование, ограничение частоты запросов)
- **Apache Kafka 3+** (асинхронная обработка сообщений)
- **OpenAI GPT-4 Vision** (AI-детектирование нарушений)
- **Docker & Docker Compose** (контейнеризация)
- **Flyway** (миграции базы данных)
- **JWT** + **OAuth2** (аутентификация)

## Возможности

### Для студентов (Роль: STUDENT)
- Регистрация/Вход через email или Google OAuth2
- Начать экзамен с использованием безопасного токена
- Просмотр статуса сессии и нарушений
- Запрос на удаление данных (GDPR)

### Для прокторов (Роль: PROCTOR)
- Создание, запуск, завершение сессий прокторинга
- Назначение сессий студентам с временными окнами
- Просмотр истории нарушений с confidence scores от AI
- Генерация PDF/JSON отчётов
- Ручное flagged нарушений

### Для администраторов (Роль: ADMIN, SUPER_ADMIN)
- Управление пользователями (активация/деактивация)
- Поиск в аудит-логах (все действия tracked)
- Настройка квот AI-запросов для каждого пользователя
- Мониторинг здоровья системы (DB, Redis, Kafka, AI статус)
- Анонимизация/удаление данных пользователей по запросу

## Установка и настройка

### Требования
- Java 17+
- Docker & Docker Compose
- OpenAI API ключ

### Локальная разработка (без Docker)
1. Клонировать репозиторий
2. Настроить файл `.env` (см. Переменные окружения)
3. Запустить вспомогательные сервисы:
   ```bash
   docker-compose up postgres redis kafka zookeeper -d
   ```
4. Собрать и запустить:
   ```bash
   ./gradlew clean bootRun
   ```
5. API доступен на `http://localhost:8080`

### Использование Docker Compose (All-in-one)
```bash
# Копировать шаблон окружения
cp .env.example .env

# Отредактировать .env и установить OPENAI_API_KEY

# Собрать и запустить все сервисы
docker-compose up --build

# Проверить статус
curl http://localhost:8080/actuator/health
```

### Миграции базы данных
Flyway автоматически выполняет миграции при старте из:
```
proctoring-infrastructure/src/main/resources/db/migration/
```

## Документация API

### Базовый URL
```
http://localhost:8080/api
```

### Endpoints аутентификации
| Метод | Endpoint          | Описание          | Публичный |
|--------|-------------------|----------------------|--------|
| POST   | /auth/register    | Регистрация нового пользователя    | ✅     |
| POST   | /auth/login       | Вход (JWT)          | ✅     |
| POST   | /auth/refresh     | Обновление токена   | ✅     |
| POST   | /auth/logout      | Выход (инвалидация)| ✅     |
| GET    | /auth/validate    | Проверка токена     | ✅     |

### Endpoints сессий (Проктор)
| Метод | Endpoint                   | Описание              | Роли |
|--------|----------------------------|--------------------------|-------|
| POST   | /sessions                  | Создать новую сессию       | PROCTOR, ADMIN |
| POST   | /sessions/{id}/start       | Запустить сессию            | PROCTOR |
| POST   | /sessions/start            | Запуск через токен (студент)| STUDENT |
| POST   | /sessions/{id}/end         | Завершить сессию              | PROCTOR |
| POST   | /sessions/{id}/cancel      | Отменить сессию           | PROCTOR |
| GET    | /sessions/{id}             | Получить детали сессии      | All   |
| GET    | /sessions                  | Список сессий            | All   |
| GET    | /sessions/verify-token     | Проверить экзаменационный токен      | All   |

### Endpoints нарушений
| Метод | Endpoint                              | Описание            | Роли |
|--------|---------------------------------------|------------------------|-------|
| GET    | /violations/session/{sessionId}       | Список нарушений        | All   |
| POST   | /violations                           | Reported нарушение       | PROCTOR |
| POST   | /violations/{id}/review               | Просмотр нарушения       | PROCTOR, ADMIN |
| GET    | /violations/stats/{sessionId}         | Статистика             | All   |

### Endpoints администратора
| Метод | Endpoint                               | Описание            | Роли |
|--------|----------------------------------------|------------------------|-------|
| GET    | /admin/users                           | Список пользователей             | ADMIN |
| PUT    | /admin/users/{id}/status               | Обновить статус          | ADMIN |
| DELETE | /admin/users/{id}/data                 | Удалить/анонимизировать       | ADMIN |
| GET    | /admin/audit-logs                      | Поиск логов            | ADMIN |
| GET    | /admin/health                          | Статус системы          | ADMIN |
| GET    | /admin/users/{id}/quota                | Получить квоту пользователя         | ADMIN |
| PUT    | /admin/users/{id}/quota                | Обновить квоту пользователя      | ADMIN |
| POST   | /admin/reports                         | Сгенерировать отчёт        | ADMIN |

### Health & Monitoring
- `GET /health` — Кастомный health check (база, redis, kafka)
- `GET /actuator/health` — Статус приложения (Spring Boot Actuator)
- `GET /actuator/info` — Информация о приложении
- `GET /actuator/metrics` — Метрики
- `GET /actuator/prometheus` — Метрики для Prometheus
- `GET /admin/health` — Детальный health статус системы (требует роль ADMIN)

## Переменные окружения

Создайте файл `.env` в корне проекта:

```bash
# База данных
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/proctoring_db
SPRING_DATASOURCE_USERNAME=proctoring_user
SPRING_DATASOURCE_PASSWORD=proctoring_pass

# JWT
JWT_SECRET=your_jwt_secret_minimum_256_bits_base64
APP_JWT_EXPIRATION=86400000
APP_JWT_REFRESH_EXPIRATION=604800000

# OpenAI
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxx

# Google OAuth2 (опционально)
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret

# AI конфигурация
AI_PROVIDER=openai
AI_OPENAI_MODEL=gpt-4o-mini
AI_TIMEOUT=30s
AI_MAX_RETRIES=3

# Ограничение частоты запросов (окно в секундах)
RATE_LIMIT_AI_REQUESTS=60
RATE_LIMIT_AI_WINDOW=60

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# CORS (опционально)
CORS_ALLOWED_ORIGINS=*
```

## Тестирование

### Unit тесты
```bash
./gradlew test
```
Цель покрытия: ≥80% (слой сервисов)

### Интеграционные тесты с Testcontainers
```bash
./gradlew integrationTest
```
Запускаются контейнеры PostgreSQL, Redis, Kafka автоматически.

### API тестирование через cURL
```bash
# Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"student@aiu.kz","username":"student1","password":"pass123","firstName":"Ershat","lastName":"Toleubaev","role":"STUDENT"}'

# Вход
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"pass123"}'

# Создать сессию (проктор)
curl -X POST http://localhost:8080/api/sessions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"<STUDENT_ID>","disciplineCode":"CS101","disciplineName":"Intro to CS","scheduledStart":"2026-04-27T10:00:00","scheduledEnd":"2026-04-27T12:00:00"}'
```

## Безопасность

### Реализованные меры
- **JWT Аутентификация** с RSA-256 подписью
- **RBAC + ABAC**: Role-based + attribute-based access control
- **Ограничение частоты запросов**: Redis sliding window (60 AI запросов/мин на пользователя)
- **Валидация ввода**: Bean Validation (JSR-380) на всех DTO
- **Предотвращение SQL-инъекций**: Параметризованные запросы через JPA
- **CORS**: Настроено для конкретных origins
- **Хеширование паролей**: BCrypt с strength 10
- **Аудит-логи**: Все действия администраторов tracked

### Соответствие OWASP Top 10
✅ Injection — Параметризованные запросы JPA
✅ Broken Auth — JWT с коротким сроком действия, refresh токены
✅ Sensitive Data Exposure — Пароли хешированы, PII зашифрованы
✅ XXE — Jackson отключает XML внешние сущности
✅ Broken Access Control — @PreAuthorize на всех endpoints
✅ Security Misconfiguration — Захардкожен production profile
✅ XSS — Санитизация ввода при выводе, Content-Security-Policy заголовки
✅ Insecure Deserialization — Безопасная конфигурация Jackson
✅ Using Vulnerable Components — Dependabot + Spring Boot BOM
✅ Logging/Monitoring — Комплексное аудит-логирование

## Соответствие GDPR

### Права субъектов данных
- **Право на доступ**: Endpoint экспорта пользовательских данных
- **Право на забвение**: Анонимизация инициирована администратором
- **Портативность данных**: JSON экспорт всех персональных данных
- **Отслеживание согласия**: Аудит-лог обработки данных

### Реализация
- Персональные данные зашифрованы при хранении (pgcrypto)
- Автоудаление логов через 30 дней (настраивается)
- Минимизация данных: Собираются только необходимые поля
- Согласие с политикой конфиденциальности при регистрации (в планах)

## Деплой

### Kubernetes Manifests
YAML файлы находятся в директории `k8s/`:
- `deployment.yaml` — Деплой бэкенда
- `service.yaml` — LoadBalancer сервис
- `ingress.yaml` — Ingress с TLS
- `configmap.yaml` — Переменные окружения
- `hpa.yaml` — Horizontal Pod Autoscaler

### CI/CD Pipeline
GitHub Actions workflow (`.github/workflows/ci-cd.yml`):
1. Build & test на PR
2. Сканирование безопасности (Trivy, Snyk)
3. Сборка Docker image & push в registry
4. Деплой в staging/production

### Требования для production
- Минимум 4 vCPU, 8GB RAM, 50GB SSD
- Kubernetes кластер (K3s рекомендован для малых деплоев)
- Managed PostgreSQL (CloudSQL, RDS) для production
- TLS/HTTPS termination на Ingress
- Внешний secrets manager (HashiCorp Vault)

## Производительность

### SLA (Service Level Agreements)
- **Время отклика API**: p95 ≤ 500 мс
- **Задержка AI анализа**: ≤ 5 секунд (асинхронно через Kafka)
- **Пропускная способность**: ≥ 100 RPS sustained
- **Доступность**: 99.9% месячный uptime

### Масштабируемость
- **Горизонтальное масштабирование**: Stateless дизайн; масштабирование через Kubernetes HPA
- **База данных**: Connection pool (Hikari) max 20 connections
- **Redis**: Cluster mode для больших деплоев
- **Kafka**: Partitioned топики для параллельной AI обработки

## Мониторинг и наблюдаемость

### Логирование
- Структурированные JSON логи (Logback)
- Уровни логирования: root=INFO, application=DEBUG
- Централизованное логирование через ELK stack (опционально)

### Метрики
- Spring Boot Actuator метрики
- Prometheus scraping endpoint
- Кастомные метрики: AI запросы, количество нарушений, длительность сессии

### Алертинг (требует настройки)
- Высокая частота ошибок (>1%)
- Медленные API ответы (>1s p95)
- Kafka consumer lag > 1000
- Подключения к базе данных > 80% capacities

## Будущие улучшения
- [ ] WebSocket для реального времени панели проктора
- [ ] Голосовой анализ и детекция речи
- [ ] Распознавание лиц для верификации личности
- [ ] Мобильное приложение-компаньон
- [ ] Многоязычные описания нарушений
- [ ] Расширенная аналитическая панель (админ)
- [ ] Детекция плагиата (сравнение текстов)

## Участники
- **Toleubayev Ershat** — Ведущий разработчик (AIU Higher School of IT & Engineering)
- **Supervisor**: Erkebulan Kayupov

---
© 2026 International University of Astana (AIU). All rights reserved.
