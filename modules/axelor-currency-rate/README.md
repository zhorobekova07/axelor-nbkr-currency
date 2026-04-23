# Модуль «Курсы валют НБКР» для Axelor ERP

## Описание
Модуль автоматически загружает ежедневные курсы валют с официального сайта
Национального банка Кыргызской Республики (НБКР) и сохраняет их в базу данных.

## Источник данных
- **URL:** https://www.nbkr.kg/XML/daily.xml
- **Формат:** XML
- **Обновление:** ежедневно

## Технологии
- Java 17
- Axelor ERP 7.2.x
- Google Guice (внедрение зависимостей)
- Quartz Scheduler (планировщик задач)
- PostgreSQL
- Gradle

## Структура модуля
axelor-currency-rate/
├── build.gradle
├── README.md
└── src/main/
├── java/com/axelor/apps/currency/
│   ├── CurrencyRateModule.java          - регистрация модуля и биндинги Guice
│   ├── db/
│   │   └── repo/
│   │       ├── CurrencyRateRepository.java      - авто-генерированный репозиторий
│   │       └── CurrencyRateBaseRepository.java  - репозиторий с доп. методами
│   ├── service/
│   │   ├── CurrencyRateService.java        - интерфейс сервиса
│   │   ├── CurrencyRateServiceImpl.java    - логика загрузки курсов с НБКР
│   │   ├── CurrencyRateBatch.java          - batch задание
│   │   └── CurrencyRateJob.java            - Quartz Job для CRON
│   └── web/
│       └── CurrencyRateController.java     - контроллер кнопок UI
└── resources/
├── domains/
│   └── CurrencyRate.xml          - описание сущности и таблицы БД
├── views/
│   ├── CurrencyRate.xml          - grid и form представления
│   └── CurrencyRateMenus.xml     - меню навигации
├── data-init/
│   └── currency-rate-schedule.sql - SQL скрипт для CRON задания
└── META-INF/
└── services/
└── com.google.inject.Module  - регистрация Guice модуля

## Поля сущности CurrencyRate

| Поле | Тип | Описание |
|------|-----|----------|
| code | String | ISO код валюты (USD, EUR...) |
| name | String | Наименование валюты |
| nominal | Integer | Номинал |
| rate | BigDecimal | Курс к KGS |
| rateDate | LocalDate | Дата курса |

## Установка

### 1. Скопировать модуль
Поместить папку `axelor-currency-rate` в директорию `modules/` проекта.

### 2. Настройка БД
В файле `axelor-config.properties`:
```properties
db.default.url = jdbc:postgresql://localhost:5432/axelor_nbkr_currency
db.default.user = postgres
db.default.password = ваш_пароль
```

### 3. Включить Quartz Scheduler
В файле `axelor-config.properties`:
```properties
quartz.enable = true
quartz.thread-count = 3
```

### 4. Собрать проект
```bash
./gradlew build -x test
```

### 5. Запустить приложение
```bash
./gradlew run -x buildFront -x installFrontDeps
```

### 6. Настроить CRON задание
Выполнить SQL скрипт в базе данных:
```sql
src/main/resources/data-init/currency-rate-schedule.sql
```

Или выполнить напрямую:
```sql
INSERT INTO meta_schedule (version, created_on, updated_on, name, job, cron, active, archived, description)
SELECT 0, NOW(), NOW(), 
       'NBKR Currency Rate Fetch', 
       'com.axelor.apps.currency.service.CurrencyRateJob', 
       '0 0 9 * * ?', 
       true, false, 
       'Daily automatic fetch of currency rates from NBKR'
WHERE NOT EXISTS (
    SELECT 1 FROM meta_schedule WHERE name = 'NBKR Currency Rate Fetch'
);
```

## Ручной запуск
В меню Axelor: **Курсы валют → Курсы валют НБКР**
Нажать кнопку **«Обновить курсы сегодня»**

## Логика обновления (Upsert)
При каждом запуске:
- Если курс за данную дату **уже есть** → обновляется
- Если курс за данную дату **отсутствует** → создаётся новая запись

Это исключает дублирование данных.

## Важные моменты реализации
- `ISOCode` в XML от НБКР является **атрибутом** тега `<Currency>`,
  поэтому читается через `el.getAttribute("ISOCode")` а не через дочерний тег
- Значение курса использует запятую как разделитель (`87,4153`),
  поэтому выполняется замена: `value.replace(",", ".")`
- CRON задание регистрируется через SQL скрипт в таблицу `meta_schedule`
