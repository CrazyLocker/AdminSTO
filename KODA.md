# KODA — карта кода и архитектуры AdminSTO

> **Назначение:** быстрый вход разработчика в кодовую базу: структура пакетов,
> ключевые классы, схема данных, тесты, CI и технический долг.
> **Актуализировано:** 18.11.2026
> **Версия ПО:** 1.2.0
> **Ревизия:** `d46404b` (ветка `develop`)

## 1. Общие сведения

AdminSTO — JavaFX-приложение на Java 21. Точка входа — `com.autoservice.App`,
запуск через `Launcher` (нужен для fat JAR). Сборка — Gradle Wrapper 8.5,
зависимости — в `versions.properties`. Данные — SQLite через HikariCP;
тесты используют H2 in-memory. Логика отделена от UI не полностью: часть
расчётов и работы с `DataStore` живёт во view-классах.

## 2. Структура пакетов

`src/main/java` — 89 файлов, 27 529 строк (подсчёт `find`/`wc -l`, 18.11.2026).

| Пакет | Файлов | Строк | Что находится |
|---|---:|---:|---|
| `com.autoservice` | 19 | 6 196 | `App`, доменные модели (`Client`, `Car`, `Service`, `SparePart`, `WorkOrder`, `Appointment`, `Setting`), слой БД (`DatabaseFactory`, `AbstractDatabase`, `SQLiteDatabase`, `H2Database`, `Database`, `DatabaseInterface`, `DatabaseSchema`, `SQLiteMigrations`), `DataStore`, `DateUtils`, `Validators`, `AppConstants` |
| `com.autoservice.views` | 14 | 9 865 | Экраны: `DashboardView`, `ClientView`, `OrderView`, `ServicePanel`, `SparePartPanel`, `StockPanel`, `AppointmentView`, `SettingsView`, `ReportView`, `ServicePartsTreeTableView`, `ServicePartsView`, `StockOperationDialog` |
| `com.autoservice.dialogs` | 12 | 4 058 | Диалоги создания/редактирования заказа, клиента, услуги, связей; общие `EntityImportDialog` / `EntityExportDialog`; печать PDF |
| `com.autoservice.services` | 13 | 3 915 | `BackupService`, `ScheduleService`, `ImportService`, `ExportService`, `StatisticsService`, `ReportGenerator`, `LoyaltyService`, `SettingService`, `TableStateManager`, `WindowStateManager`, `JsonStateManager`, `FileDialogPathManager`, `AutoAddSparePartService` |
| `com.autoservice.utils` | 13 | 1 972 | `ThemeManager`, `ThemePreferences`, `IconHelper`, `Formatters`, `StockHelper`, `ValidationUtils`, `ValidationErrorIndicator`, `ExceptionHandler`, `UserFriendlyErrorMessage`, `LoggerManager`, `DialogValidator`, `TooltipHelper`, `ServicePartHelper` |
| `com.autoservice.model` | 12 | 834 | DTO состояния (`TableState`, `ColumnState`, `SortState`, `WindowState`), связи (`ServicePart`, `ServiceSparePart`, `ServiceSparePartsList`), `ServiceCategory`, строки таблиц настроек |
| `com.autoservice.controllers` | 6 | 689 | Тонкие контроллеры между view и `DataStore`: клиенты, заказы, услуги, запчасти, склад |

`src/test/java` — 76 файлов, 14 817 строк: 55 классов юнит-тестов и 10 классов
UI-тестов (плюс абстрактный `BaseUITest` и хелперы).

## 3. Ключевые классы

### Запуск и жизненный цикл

- `App` — создаёт 8 вкладок, применяет тему до создания сцены, инициализирует
  БД и `ScheduleService`, при закрытии сохраняет окно, таблицы и данные.
- `Launcher` — обёртка `main` для fat JAR.
- `DatabaseFactory` — ленивая инициализация и закрытие БД, `initForTest()` для H2.
- `DataStore` — singleton-кэш `ObservableList` клиентов, услуг, запчастей,
  заказов, записей и связей; `load()`, `save()`, CRUD-методы. Удаление из кэша
  идёт по `id`, а не по ссылке.

### Слой БД

- `DatabaseInterface` — контракт доступа к данным.
- `AbstractDatabase` — общая реализация CRUD, загрузки агрегатов и работы с JDBC.
- `SQLiteDatabase` / `H2Database` — реализации для продакшена и тестов.
- `DatabaseSchema` — единое DDL всех таблиц и индексов, параметризованное
  диалектом автоинкремента.
- `SQLiteMigrations` — миграции существующих баз (добавление колонок,
  переименование legacy-таблиц, перенос данных).

### Бизнес-логика

- `LoyaltyService` — звёзды ТО, Stage тюнинга, скидки; все числа читаются из
  `app_settings` с значениями по умолчанию.
- `BackupService` — ZIP-бэкап БД и состояний UI; проверка архива, атомарная
  замена, откат при ошибке.
- `ScheduleService` — ежедневный автобэкап по настройкам.
- `ImportService` / `ExportService` — XML/CSV/JSON; `ImportService` сохраняет
  `double`-остатки (`12.0`) и ищет клиента по телефону в человеческом формате.
- `TableStateManager` / `WindowStateManager` — JSON-состояния в `config/`.
- `ThemeManager` / `ThemePreferences` — выбор темы в `config/theme.txt`,
  применение при старте. Доступно 7 тем AtlantaFX.

### UI

- `DashboardView` — сводка и быстрые действия, вход в отчёт.
- `ClientView` — список клиентов, поиск, карточка с автомобилями.
- `OrderView` — заказы, фильтры, статусы, PDF и детали.
- `AppointmentView` — календарь «Запись»: недельная/месячная сетка, легенда,
  карточки записей, цветная полоса статуса заказа, счётчики в заголовках дней.
- `SettingsView` — 8 вкладок; содержит крупные методы сборки панелей.
- `ServicePartsTreeTableView` — дерево связей «услуга → запчасти».
- `ThemeManager.applySceneStyles(scene)` подключает `global-custom.css` и
  `dashboard-custom.css`; диалоги заказа и связей вызывают его же, поэтому
  «зебра» и подсветка выбора действуют и в них.

## 4. Модель данных

Схема — `DatabaseSchema` (14 таблиц):

| Таблица | Назначение |
|---|---|
| `clients` | Клиенты: имя, фамилия, телефон, legacy-поля авто, звёзды ТО, сумма тюнинга |
| `client_cars` | Автомобили клиента: модель, номер, пробег |
| `services` | Услуги: цена, длительность, категория, параметры масла/запчасти |
| `spare_parts` | Запчасти: цены, остаток, минимум, место, категория, единица |
| `orders` | Заказы: строковый `id`, клиент, статус, сумма, дата, пробег, заметки |
| `order_services` | Услуги в заказе (имя, цена, ссылка на услугу) |
| `order_parts` | Запчасти в заказе (имя, цена, количество, закупка, единица) |
| `appointments` | Записи календаря: клиент, мастер, услуга, дата, время, статус, заказ |
| `service_spare_parts` | Legacy-связи «услуга — запчасть» с количеством и активностью |
| `to_parts` | Расходники ТО по модели автомобиля |
| `app_settings` | Настройки приложения «ключ — значение — описание» |
| `service_spare_parts_lists` | Списки запчастей для услуги (новая структура) |
| `service_spare_parts_list_items` | Элементы таких списков |
| `service_parts` | Связи «услуга → запчасть» для дерева в настройках |

Даты в БД — `dd/MM/yyyy`; в UI — `d MMMM yyyy 'г.'`. Идентификатор заказа —
строка `ZAK-ДД/ММ/ГГ-NNNN`. Статусы заказа: «Новый», «В работе», «Закрыт»;
статусы записи: «Новая», «Подтверждена», «Выполнено», «Отменена».

## 5. Файлы состояния и конфигурация

| Путь | Содержимое |
|---|---|
| `autoservice.db` | База SQLite (по умолчанию в рабочей директории) |
| `config/theme.txt` | Идентификатор темы, например `primer-light` |
| `config/table-state/<tableId>.json` | Ширина, порядок, видимость и сортировка колонок |
| `config/window-state/<windowId>.json` | Позиция и размер окна |
| `logs/` | Логи с ротацией |
| `backups/` | ZIP-бэкапы |

## 6. Тестовая инфраструктура

- `./gradlew test` — 571 юнит-тест в 55 классах; `forkEvery = 1`, поэтому
  каждый класс получает свежую JVM (изоляция статики `DataStore`).
- `./gradlew uiTest` — 51 UI-тест в 10 классах; headless Monocle.
- `JavaFxTestSupport` — `Platform.startup`, `setImplicitExit(false)`,
  выполнение действий на FX-потоке с таймаутами и закрытие модальных диалогов
  фоновой задачей. Через него включены 4 ранее отключённых теста.
- `BaseUITest` — собственные хелперы поиска/клика/ввода без TestFX.
- JaCoCo-пороги: `services` 75 %, `utils` 29 %, `model` 55 %.
- PIT: test strength 70 %, запуск вручную/через `workflow_dispatch`.

Подробности — в [TEST_REPORT.md](docs/TEST_REPORT.md),
[COVERAGE_AND_MUTATION.md](docs/COVERAGE_AND_MUTATION.md) и
[UI_testplan.md](UI_testplan.md).

## 7. CI и релиз

- `.github/workflows/ci.yml`:
  - `build` — JDK 21, `./gradlew build`, отчёты JaCoCo и артефакты;
  - `ui-test` — Xvfb + `./gradlew uiTest`, после успешного `build`;
  - `mutation` — PIT только по `workflow_dispatch`.
- `.github/workflows/release.yml` — по тегу `v*` на `windows-latest`:
  `build` → `jpackage` → ZIP `build/jpackage/AdminSTO` → GitHub Release.
- Версия приложения — `version = '1.2.0'` в `build.gradle`.

## 8. Технический долг и риски

1. **Крупные классы.** `SettingsView` — 2 961 строка, `AbstractDatabase` —
   2 012, `DashboardView` — 1 975, `AppointmentView` — 1 395. В них много
   длинных методов; часть логики стоит вынести в сервисы.
2. **Слабое покрытие UI в `test`.** `views` и `dialogs` проверяются только
   `uiTest`, поэтому общий процент JaCoCo низкий (34,5 %). Это осознанное
   решение, а не регрессия.
3. **Слой БД в корневом пакете.** `AbstractDatabase`, `SQLiteDatabase`,
   `H2Database`, `DatabaseInterface`, `DatabaseSchema` — около 6 000 строк;
   логично вынести в `com.autoservice.db`.
4. **Контроллеры почти не используются.** View-классы обращаются к
   `DataStore`/`DatabaseFactory` напрямую.
5. **Нет аутентификации и ролей.** Приложение рассчитано на доверенное
   рабочее место; аудита действий нет.
6. **Дублирование связей.** В дереве «Связи» (`service_parts`) диалог
   добавления вставляет новую строку; обновление существующей пары
   реализовано для legacy-таблицы `service_spare_parts`
   (`DataStore.addServiceSparePart` — `UPDATE` по `id`).

Актуальный разбор с приоритетами — в [docs/CODE_REVIEW.md](docs/CODE_REVIEW.md).

## 9. Куда смотреть дальше

| Задача | Документ |
|---|---|
| Понять требования и схему | [docs/TECHNICAL_SPECIFICATION.md](docs/TECHNICAL_SPECIFICATION.md) |
| Оценить готовность релиза | [docs/RELEASE_READINESS.md](docs/RELEASE_READINESS.md) |
| Разобраться в лояльности | [docs/LOYALTY_SYSTEM.md](docs/LOYALTY_SYSTEM.md) |
| Настроить связи услуг и запчастей | [docs/InstructionForService+Spareparts.md](docs/InstructionForService+Spareparts.md) |
| Добавить таблицу с сохранением состояния | [docs/table-settings-guide.md](docs/table-settings-guide.md) |
| Залить демо-данные | [docs/SEED_TEST_DATA.md](docs/SEED_TEST_DATA.md) |
