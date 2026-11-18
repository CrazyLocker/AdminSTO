# Техническая спецификация системы AdminSTO

## Административная система управления автосервисом

> **Назначение:** функциональные и технические требования, архитектура, модель
> данных и условия развёртывания версии 1.2.0.
> **Актуализировано:** 18.11.2026
> **Версия ПО:** 1.2.0
> **Статус:** актуален

---

## Содержание

1. [Общее описание](#1-общее-описание)
2. [Архитектура системы](#2-архитектура-системы)
3. [Технологический стек](#3-технологический-стек)
4. [Функциональные требования](#4-функциональные-требования)
5. [Модель данных](#5-модель-данных)
6. [Интерфейсы и API](#6-интерфейсы-и-api)
7. [Требования к безопасности](#7-требования-к-безопасности)
8. [Требования к производительности](#8-требования-к-производительности)
9. [Тестирование](#9-тестирование)
10. [Развёртывание](#10-развёртывание)
11. [Приложения](#11-приложения)

---

## 1. Общее описание

### 1.1. Назначение системы

AdminSTO — настольное приложение для автоматизации работы автосервиса:
клиенты и автомобили, заказы-наряды, справочники услуг и запчастей, склад,
календарь записей, лояльность, отчётность и резервное копирование. Данные
хранятся локально в SQLite; сетевого взаимодействия нет.

### 1.2. Область применения

- ведение клиентской базы и автомобилей клиента;
- оформление и сопровождение заказов-нарядов;
- учёт услуг, запчастей и остатков;
- планирование записей в календаре;
- скидки по программам лояльности;
- отчёты, PDF-печать и резервные копии.

### 1.3. Целевая аудитория

Администраторы автосервиса, мастера-приёмщики, руководители СТО.

### 1.4. Определения и сокращения

| Термин | Определение |
|---|---|
| Заказ-наряд | Документ с работами и запчастями по обслуживанию автомобиля |
| Запись | Запланированное посещение сервиса клиентом |
| ТО | Техническое обслуживание (категория `MAINTENANCE`) |
| Тюнинг | Категория услуг/запчастей `TUNING` |
| `DataStore` | Единый кэш и точка доступа к данным приложения |
| `AppSettings` | Таблица `app_settings` с настройками «ключ — значение» |

---

## 2. Архитектура системы

### 2.1. Слои

```
Presentation  — views/ (экраны), dialogs/ (модальные окна)
Controller    — controllers/ (тонкие обёртки refresh/CRUD)
Service       — services/ (бэкапы, импорт/экспорт, отчёты, лояльность,
                состояние таблиц/окон, планировщик, темы)
Data          — DataStore, DatabaseFactory, AbstractDatabase,
                SQLiteDatabase/H2Database, DatabaseSchema, SQLiteMigrations
Model         — доменные классы и DTO (Client, WorkOrder, Appointment,
                ServicePart, TableState, WindowState и др.)
Utility       — DateUtils, Validators, Formatters, IconHelper,
                ThemeManager, ExceptionHandler
```

`App` собирает 8 вкладок, применяет тему до создания сцены, инициализирует
`DatabaseFactory` и `ScheduleService`. При закрытии сохраняются окно, состояния
таблиц и данные `DataStore`.

### 2.2. Компоненты

| Компонент | Ответственность |
|---|---|
| `DashboardView` | Сводка, быстрые действия, вход в отчёт |
| `ClientView` | Клиенты и автомобили, поиск |
| `OrderView` | Заказы, фильтры, статусы, PDF |
| `ServicePanel`, `SparePartPanel`, `StockPanel` | Справочники и склад |
| `AppointmentView` | Календарь «Запись»: неделя/месяц |
| `SettingsView` | 8 вкладок настроек |
| `ReportView` | Сводный отчёт |
| `DataStore` | Кэш `ObservableList` и CRUD-фасад |
| `AbstractDatabase` + реализации | JDBC, загрузка агрегатов, миграции |
| `LoyaltyService` | Звёзды ТО, Stage тюнинга, скидки |
| `BackupService` / `ScheduleService` | Бэкапы и расписание |
| `TableStateManager` / `WindowStateManager` | JSON-состояния интерфейса |
| `ThemeManager` / `ThemePreferences` | Выбор и применение темы |

### 2.3. Потоки данных

1. `DatabaseFactory.init()` открывает пул HikariCP.
2. `DataStore.load()` загружает справочники и документы в `ObservableList`.
3. View/контроллеры читают кэш и вызывают CRUD-методы `DataStore`, которые
   пишут в БД и обновляют кэш.
4. При закрытии `DataStore.save()` сохраняет «грязные» объекты, затем пул
   закрывается.

---

## 3. Технологический стек

### 3.1. Основные технологии

| Компонент | Версия |
|---|---|
| Java | 21 |
| JavaFX | 21.0.6 |
| Gradle Wrapper | 8.5 |
| SQLite JDBC | 3.44.1.0 |
| HikariCP | 5.0.1 |
| iText 7 | 7.2.5 |
| Gson | 2.10.1 |
| AtlantaFX | 2.1.0 |
| JUnit 5 | 5.10.1 |
| AssertJ | 3.25.1 |
| Mockito | 5.8.0 |
| H2 (тесты) | 2.2.224 |
| TestFX / Monocle | 4.0.18 / 21.0.2 |
| PIT | 1.30.2 (плагин 1.19.0) |

Версии зависимостей вынесены в `versions.properties`; версия приложения —
`version = '1.2.0'` в `build.gradle`.

### 3.2. Требования к окружению

| Параметр | Требование |
|---|---|
| ОС | Windows 10/11, Linux, macOS |
| Java | JDK 21 для сборки, JRE 21 для запуска |
| ОЗУ | минимум 512 МБ |
| Диск | минимум 100 МБ (JAR) / ~1 ГБ (сборка с Gradle) |
| Разрешение | минимум 1280×720 |

---

## 4. Функциональные требования

### 4.1. Клиенты и автомобили

| ID | Требование | Статус |
|---|---|---|
| REQ-CL-001 | Добавление клиента (имя, фамилия, телефон) | ✅ |
| REQ-CL-002 | Хранение нескольких автомобилей клиента (`client_cars`) | ✅ |
| REQ-CL-003 | Хранение модели, госномера и пробега автомобиля | ✅ |
| REQ-CL-004 | Редактирование и удаление клиента с подтверждением | ✅ |
| REQ-CL-005 | Валидация телефона и госномера РФ | ✅ |
| REQ-CL-006 | Отображение телефона в формате `+7 (XXX) XXX-XX-XX` | ✅ |
| REQ-CL-007 | Поиск по ФИО, телефону, автомобилю | ✅ |
| REQ-CL-008 | Показ звёзд ТО и статуса тюнинга в карточке | ✅ |

Телефон хранится как `+7XXXXXXXXXX`, в UI вводится только цифрами после `+7`.

### 4.2. Услуги и запчасти

| ID | Требование | Статус |
|---|---|---|
| REQ-SV-001 | CRUD услуг: название, цена, длительность | ✅ |
| REQ-SV-002 | Категория услуги `REGULAR` / `MAINTENANCE` / `TUNING` | ✅ |
| REQ-SV-003 | Параметры масла и автоматически добавляемой запчасти | ✅ |
| REQ-SP-001 | CRUD запчастей: артикул, производитель, совместимость | ✅ |
| REQ-SP-002 | Цены закупки/розничная, остаток, минимальный запас, место | ✅ |
| REQ-SP-003 | Единица измерения `шт` / `л` / `компл` | ✅ |
| REQ-SP-004 | Категория запчасти `REGULAR` / `MAINTENANCE` / `TUNING` | ✅ |
| REQ-SP-005 | Связи «услуга → запчасть» в `service_parts` | ✅ |
| REQ-SP-006 | Автодобавление связанных запчастей в заказ | ✅ |

### 4.3. Заказы

| ID | Требование | Статус |
|---|---|---|
| REQ-WO-001 | Создание заказа с клиентом и автомобилем | ✅ |
| REQ-WO-002 | Несколько услуг и запчастей с количеством | ✅ |
| REQ-WO-003 | Расчёт суммы с учётом скидок по категориям | ✅ |
| REQ-WO-004 | Статусы «Новый», «В работе», «Закрыт» | ✅ |
| REQ-WO-005 | Списание и возврат остатков | ✅ |
| REQ-WO-006 | Редактирование и удаление с подтверждением | ✅ |
| REQ-WO-007 | Экспорт заказа-наряда в PDF | ✅ |
| REQ-WO-008 | Фильтры по клиенту, статусу, сумме, поиск | ✅ |
| REQ-WO-009 | Закрытый заказ не редактируется | ✅ |

При закрытии заказа `LoyaltyService` начисляет звёзды ТО и сумму тюнинга;
исторические суммы заказов не пересчитываются.

### 4.4. Календарь «Запись»

| ID | Требование | Статус |
|---|---|---|
| REQ-AP-001 | Создание записи: клиент, мастер, услуга, дата, время | ✅ |
| REQ-AP-002 | Переключатель «Неделя / Месяц» | ✅ |
| REQ-AP-003 | Навигация по периодам и кнопка «Сегодня» | ✅ |
| REQ-AP-004 | Карточка записи: клиент, автомобиль, услуга, мастер | ✅ |
| REQ-AP-005 | Цветная полоса статуса связанного заказа | ✅ |
| REQ-AP-006 | Легенда статусов | ✅ |
| REQ-AP-007 | Счётчики записей в заголовках дней | ✅ |
| REQ-AP-008 | Статусы «Новая», «Подтверждена», «Выполнено», «Отменена» | ✅ |
| REQ-AP-009 | Создание записи двойным кликом по свободной ячейке | ✅ |
| REQ-AP-010 | Фильтрация по дате и мастеру | ✅ |

Слоты времени: 08:00–20:00. Мастера: `AppConstants.MASTERS`.

### 4.5. Лояльность

| ID | Требование | Статус |
|---|---|---|
| REQ-LY-001 | Звезда ТО за закрытый заказ с услугой ТО и пробегом 5 000–10 000 км | ✅ |
| REQ-LY-002 | Максимум 5 звёзд; скидки 5/10/15 % при 3/4/5 звёздах | ✅ |
| REQ-LY-003 | Stage 1/2/3 тюнинга: ≥30 000 / >100 000 / >300 000 ₽ | ✅ |
| REQ-LY-004 | Скидки Stage 5/10/15 % | ✅ |
| REQ-LY-005 | Раздельные скидки по категориям строк, без суммирования | ✅ |
| REQ-LY-006 | Параметры читаются из `app_settings` с значениями по умолчанию | ✅ |

### 4.6. Оформление и таблицы

| ID | Требование | Статус |
|---|---|---|
| REQ-UI-001 | 7 тем AtlantaFX, выбор на вкладке «Внешний вид» | ✅ |
| REQ-UI-002 | Тема сохраняется в `config/theme.txt` и применяется при старте | ✅ |
| REQ-UI-003 | Чередование строк таблиц («зебра») во всех таблицах | ✅ |
| REQ-UI-004 | Голубая подсветка выбранной строки | ✅ |
| REQ-UI-005 | Общие стили в диалогах заказа и связей | ✅ |
| REQ-UI-006 | Сохранение ширины/порядка/сортировки колонок | ✅ |
| REQ-UI-007 | Восстановленное окно ограничивается границами экрана | ✅ |

### 4.7. Настройки

| ID | Вкладка | Содержание |
|---|---|---|
| REQ-SET-001 | Внешний вид | Выбор темы, подсказка о перезапуске |
| REQ-SET-002 | Настройки приложения | Таблица ключей `app_settings` |
| REQ-SET-003 | Автозаполнение | Правила автодобавления запчастей |
| REQ-SET-004 | Лояльность | Интервалы ТО, звёзды и скидки, пороги Stage |
| REQ-SET-005 | Связи | Дерево «услуга → запчасти», CRUD |
| REQ-SET-006 | Расходники ТО | Набор запчастей по модели, 13 моделей GWM + ручной ввод |
| REQ-SET-007 | Резервное копирование | Автобэкап, время, количество, создание/восстановление |
| REQ-SET-008 | Импорт/Экспорт | Импорт и экспорт XML |

### 4.8. Резервное копирование и данные

| ID | Требование | Статус |
|---|---|---|
| REQ-BU-001 | ZIP-бэкап с БД, `config/table-state`, `config/window-state` | ✅ |
| REQ-BU-002 | Ручной и автоматический бэкап по расписанию | ✅ |
| REQ-BU-003 | Ограничение количества хранимых копий | ✅ |
| REQ-BU-004 | Проверка архива и атомарная замена БД | ✅ |
| REQ-BU-005 | Откат из копии при ошибке восстановления | ✅ |
| REQ-DT-001 | Импорт клиентов/услуг/запчастей из XML | ✅ |
| REQ-DT-002 | Экспорт справочников в XML | ✅ |

### 4.9. Отчётность

| ID | Требование | Статус |
|---|---|---|
| REQ-RP-001 | Сводный отчёт по заказам, статусам, выручке | ✅ |
| REQ-RP-002 | Топ услуг и запчастей | ✅ |
| REQ-RP-003 | Программный PDF-экспорт отчёта (`ReportGenerator`) | ✅ |
| REQ-RP-004 | Печать заказа-наряда в PDF | ✅ |

---

## 5. Модель данных

### 5.1. Схема базы данных

DDL хранится в `DatabaseSchema` и одинаков для SQLite и H2 (различается только
синтаксис автоинкремента). Всего **14 таблиц**.

| Таблица | Ключевые поля | Назначение |
|---|---|---|
| `clients` | id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total | Клиенты и агрегаты лояльности |
| `client_cars` | id, client_id, car_model, car_number, mileage | Автомобили клиента |
| `services` | id, name, price, duration, part_number, category, oil_volume, uses_oil, spare_part_name, spare_part_quantity | Услуги |
| `spare_parts` | id, name, part_number, manufacturer, compatible_models, note, purchase_price, retail_price, stock, min_stock, location, category, unit_type | Запчасти |
| `orders` | id TEXT, client_id, status, total, created_date, mileage, closed_date, notes | Заказы |
| `order_services` | order_id, service_name, price, service_id | Услуги в заказе |
| `order_parts` | order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price | Запчасти в заказе |
| `appointments` | id, client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status | Записи календаря |
| `service_spare_parts` | id, service_id, spare_part_id, quantity, unit_type, active | Legacy-связи |
| `to_parts` | id, car_model, spare_part_id, quantity, unit_type, note, active | Расходники ТО по модели |
| `app_settings` | id, setting_key, setting_value, description | Настройки |
| `service_spare_parts_lists` | id, service_id, created_date, active | Legacy-списки запчастей |
| `service_spare_parts_list_items` | id, list_id, spare_part_id, quantity, unit_type | Элементы legacy-списков |
| `service_parts` | id, service_id, spare_part_id, quantity, is_required, created_date | Основные связи «услуга → запчасть» |

Индексы создаются по внешним ключам, датам, статусам, именам и связям —
перечень в `DatabaseSchema.INDEXES`.

### 5.2. Форматы

| Данные | Формат |
|---|---|
| Дата в БД | `dd/MM/yyyy` |
| Дата в UI | `d MMMM yyyy 'г.'` |
| Дата и время записи | `dd/MM/yyyy HH:mm` |
| Идентификатор заказа | `ZAK-ДД/ММ/ГГ-NNNN` |
| Телефон | хранение `+7XXXXXXXXXX`, отображение `+7 (XXX) XXX-XX-XX` |
| Госномер | верхний регистр, допустимые буквы РФ |

### 5.3. Статусы

| Сущность | Значения |
|---|---|
| Заказ | «Новый», «В работе», «Закрыт» |
| Запись | «Новая», «Подтверждена», «Выполнено», «Отменена» |
| Категория | `REGULAR`, `MAINTENANCE`, `TUNING` |
| Тема | `primer-light`, `primer-dark`, `nord-light`, `nord-dark`, `cupertino-light`, `cupertino-dark`, `dracula` |

### 5.4. Основные модели

`Client`, `Car`, `Service`, `SparePart`, `WorkOrder`, `Appointment`, `Setting`,
`ServicePart`, `ServiceSparePart`, `ServiceSparePartsList`,
`ServiceSparePartsListItem`, `ToPart`, `TableState`, `ColumnState`, `SortState`,
`WindowState`.

`ColumnState` и `SortState` — records; `ColumnState.index` допускает `null`
(старый JSON), такие колонки ставятся в конец при восстановлении.

---

## 6. Интерфейсы и API

### 6.1. DatabaseInterface

Контракт доступа к данным: `connect/createTables/close`, CRUD клиентов,
услуг, запчастей, заказов, записей, настроек, связей и списков, загрузка
агрегатов (`getAllOrders`, `getAppointmentsByDate` и др.), миграции.
Реализации — `AbstractDatabase` → `SQLiteDatabase` / `H2Database`.

### 6.2. DataStore

Singleton-кэш `ObservableList` и CRUD-фасад: `init/load/save`,
`addClient/updateClient/deleteClient`, `addOrder/updateOrder/deleteOrder`,
`addAppointment/updateAppointment`, `addServicePart/updateServicePart/
deleteServicePart`, `addServiceSparePart` (обновляет существующую пару),
`getSettingByKey`, `getAppointmentsByDate`, `markDirty`. Удаление из кэша
выполняется по `id`.

### 6.3. TableStateManager

```java
public static void saveTableState(Object table, String tableId);
public static void loadTableState(Object table, String tableId);
public static void resetTableState(String tableId);
public static boolean hasSavedState(String tableId);
public static void setStorageDirForTest(String dir);
```

Хранилище — `config/table-state/<tableId>.json`; сохраняются ширина (через
`getWidth()`), видимость, порядок и сортировка.

### 6.4. WindowStateManager

`saveWindowState(...)`, `restoreWindowState(...)`, `resetWindowState(...)`,
`hasSavedState(...)`. Файлы — `config/window-state/*.json`; при восстановлении
размер ограничивается `Screen.getPrimary().getVisualBounds()`.

### 6.5. ThemeManager

`AppTheme` (7 значений), `getSavedTheme`, `saveTheme`, `applySavedTheme`,
`applySceneStyles(Scene)`, `getAvailableThemes`. Выбор хранится в
`config/theme.txt`; динамическая смена темы не поддерживается.

### 6.6. BackupService

`createBackup()`, `createBackup(sourceDb, backupDir, tableStateDir,
windowStateDir)`, `restoreBackup(path)`, `getAvailableBackups()`,
`getLatestBackupPath()`, `cleanupOldBackups(count)`. Восстановление:
проверка архива → распаковка во временный файл → бэкап текущей БД → закрытие
пула → атомарная замена → переинициализация и `DataStore.load()`.

---

## 7. Требования к безопасности

| Требование | Статус |
|---|---|
| Локальное хранение данных (SQLite) | ✅ |
| Отсутствие сетевого доступа | ✅ |
| PreparedStatement против SQL-инъекций | ✅ |
| Валидация входных данных | ✅ |
| Аутентификация и роли | ❌ не реализованы |
| Журнал аудита | ❌ не реализован |
| Шифрование БД | ❌ не реализовано |

Приложение рассчитано на доверенное рабочее место; доступ ограничивается
правами ОС. Для коммерческого развёртывания рекомендуется добавить
аутентификацию, роли и аудит.

---

## 8. Требования к производительности

| Метрика | Ориентир |
|---|---|
| Запуск | до 5 секунд |
| Загрузка данных | до 2 секунд |
| Отклик UI | до 100 мс |
| Клиенты | до 10 000 |
| Заказы | до 50 000 |

Механизмы: пул HikariCP, индексы, кэш `DataStore`, пакетные операции JDBC.
UI-тесты и ручные проверки выполняются на демо-данных (12 клиентов,
22 заказа, записи календаря).

---

## 9. Тестирование

| Уровень | Инструмент | Количество |
|---|---|---|
| Юнит/интеграционные | JUnit 5, AssertJ, Mockito, H2 | **571 тест в 55 классах** |
| UI | JavaFX + Monocle, `./gradlew uiTest` | **51 тест в 10 классах** |
| Мутации | PIT | 1 084 мутации, 595 убито, test strength 72 % |

Пороги JaCoCo: `services` 75 %, `model` 55 %, `utils` 29 %.
Подробные цифры и методика — в [TEST_REPORT.md](TEST_REPORT.md) и
[COVERAGE_AND_MUTATION.md](COVERAGE_AND_MUTATION.md).

```bash
./gradlew test                 # 571 тест
./gradlew uiTest               # 51 UI-тест
./gradlew jacocoTestReport     # покрытие
./gradlew pitest               # мутации
```

---

## 10. Развёртывание

### 10.1. Сборка

```bash
./gradlew build      # сборка, тесты, SpotBugs, Checkstyle, пороги JaCoCo
./gradlew fatJar     # автономный JAR
./gradlew jpackage   # app-image build/jpackage/AdminSTO
./gradlew run        # запуск из исходников
```

### 10.2. Релиз

`.github/workflows/release.yml` по тегу `v*` на `windows-latest`:
`build` → `jpackage` → ZIP `build/jpackage/AdminSTO` →
`AdminSTO-Windows.zip` → GitHub Release.

### 10.3. Структура поставки

```
AdminSTO/
├── AdminSTO.exe            # лаунчер портативной сборки (Windows)
├── app/                    # JAR и зависимости
├── runtime/                # встроенный JRE 21
├── autoservice.db          # БД (создаётся при первом запуске)
├── config/
│   ├── table-state/*.json
│   ├── window-state/*.json
│   └── theme.txt
├── logs/
└── backups/
```

### 10.4. Конфигурация

| Параметр | По умолчанию | Описание |
|---|---|---|
| Файл БД | `autoservice.db` | Рабочая директория |
| `config/theme.txt` | `primer-light` | Выбранная тема |
| `config/table-state/` | — | Состояния таблиц |
| `config/window-state/` | — | Состояния окон |
| `logs/` | — | Логи с ротацией 30 дней / 100 МБ |

---

## 11. Приложения

### 11.1. Ссылки на документы

| Документ | Описание |
|---|---|
| [README.md](../README.md) | Обзор и карта документации |
| [INSTRUCTIONS.md](../INSTRUCTIONS.md) | Руководство пользователя |
| [KODA.md](../KODA.md) | Карта кода |
| [UI_testplan.md](../UI_testplan.md) | План UI-тестов |
| [TEST_REPORT.md](TEST_REPORT.md) | Отчёт о тестировании |
| [TRACEABILITY_MATRIX.md](TRACEABILITY_MATRIX.md) | Матрица трассируемости |
| [COVERAGE_AND_MUTATION.md](COVERAGE_AND_MUTATION.md) | Покрытие и мутации |
| [RELEASE_READINESS.md](RELEASE_READINESS.md) | Готовность к релизу |
| [LOYALTY_SYSTEM.md](LOYALTY_SYSTEM.md) | Лояльность |
| [InstructionForService+Spareparts.md](InstructionForService+Spareparts.md) | Связи услуг и запчастей |
| [table-settings-guide.md](table-settings-guide.md) | Состояние таблиц |
| [SEED_TEST_DATA.md](SEED_TEST_DATA.md) | Демо-данные |

### 11.2. История изменений

| Версия | Дата | Изменения |
|---|---|---|
| 0.1.0 | 01.2026 | Начальная версия |
| 0.1.5 | 03.2026 | Записи на обслуживание |
| 0.1.9 | 05.2026 | Отчётность, исправления |
| 1.0.0 | 07.2026 | Первый стабильный релиз |
| 1.2.0 | 11.2026 | Календарь «Запись», «зебра» и подсветка таблиц, 7 тем, jpackage и релизный workflow, скрипты демо-данных |

### 11.3. Контакты

| Роль | Контакт |
|---|---|
| Разработка | NLP-Core-Team |
| Репозиторий | https://github.com/CrazyLocker/AdminSTO |

---

## Глоссарий

| Термин | Определение |
|---|---|
| CRUD | Create, Read, Update, Delete |
| JDBC | Java Database Connectivity |
| JavaFX | GUI-фреймворк Java |
| SQLite | Встраиваемая реляционная БД |
| H2 | In-memory БД для тестов |
| JaCoCo | Измерение покрытия |
| PIT | Мутационное тестирование |
| Monocle | Headless-платформа JavaFX |
| Table State | Ширина, порядок, видимость и сортировка колонок |
| jpackage | Утилита JDK для app-image со встроенным JRE |
