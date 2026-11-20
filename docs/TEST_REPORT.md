# Отчёт о тестировании системы автосервиса AdminSTO 1.2.0

> **Назначение:** текущие результаты юнит-, интеграционных и UI-тестов,
> покрытия и мутационного анализа.
> **Актуализировано:** 18.11.2026
> **Версия ПО:** 1.2.0
> **Ревизия:** `d46404b` (`develop`)

## Общая информация

| Параметр | Значение |
|---|---|
| Проект | `autoservice-admin` |
| Версия | 1.2.0 |
| JDK | 21 (Temurin) |
| Gradle Wrapper | 8.5 |
| JUnit | 5.10.1 |
| Дата прогона | 18.11.2026, Linux, headless Monocle |
| Юнит/интеграционные тесты | **571 в 55 классах** |
| UI-тесты | **51 в 10 классах** |
| Провалы / ошибки / пропуски | **0 / 0 / 0** |
| `@Disabled` | **0** |

## Как воспроизвести

```bash
./gradlew clean test            # 571 тест
./gradlew uiTest                # 51 UI-тест
./gradlew jacocoTestReport      # build/reports/jacoco/test/html/index.html
./gradlew jacocoTestCoverageVerification
./gradlew pitest                # build/reports/pitest/index.html
./gradlew build                 # всё выше + SpotBugs + Checkstyle
```

## Резюме

| Метрика | Значение |
|---|---|
| Всего юнит/интеграционных тестов | 571 |
| Успешные | 571 (100 %) |
| Проваленные | 0 |
| Ошибки | 0 |
| Пропущенные | 0 |
| UI-тестов | 51 |
| UI-провалов | 0 |
| Покрытие строк (проект) | 34,5 % |
| Покрытие инструкций | 32,2 % |
| Покрытие ветвей | 27,4 % |
| JaCoCo-пороги | ✅ `services` 88,1 %, `model` 88,3 %, `utils` 31,2 % |
| PIT test strength | 72 % при пороге 70 % |

## Результаты по группам

### Юнит/интеграционные тесты (571)

| Группа | Тестов | Классы |
|---|---:|---|
| Модели и DTO | 106 | `ClientTest`, `ServiceTest`, `SparePartTest`, `WorkOrderTest`, `AppointmentTest`, `AppointmentLegacyClientTest`, `WorkOrderLoyaltyDiscountTest`, `NullSafetyTest`, `ServiceSparePartsRowTest`, `AppConstantsTest` |
| БД и `DataStore` | 120 | `DatabaseTest`, `DatabaseSchemaTest`, `SQLiteMigrationsTest`, `DataStoreTest`, `DataStoreRelationsTest`, `DataStorePersistenceTest`, `DataStoreNullClientTest`, `ParallelDatabaseTest`, `ServiceSparePartsListTest` |
| Сервисы | 151 | `BackupService*`, `ImportService*`, `ExportServiceTest`, `SettingServiceTest`, `ScheduleServiceTest`, `StatisticsServiceTest`, `ReportGeneratorTest`, `LoyaltyServiceTest`, `TableStateManagerTest`, `JsonStateManagerTest`, `FileDialogPathManagerTest`, `AutoAddSparePartServiceTest` |
| Утилиты | 69 | `DateUtilsTest`, `ValidationUtilsTest`, `FormattersTest`, `StockHelperTest`, `ThemeManagerTest`, `ThemePreferencesTest` |
| Контроллеры | 42 | `ClientControllerTest`, `OrderControllerTest`, `ServicePanelControllerTest`, `SparePartPanelControllerTest`, `StockPanelControllerTest` |
| Диалоги | 54 | `CreateOrderDialogTest`, `EditOrderDialogTest`, `EditClientDialogTest`, `EntityExportDialogTest`, `ImportSparePartsDialogTest`, `PrintOrderDialogTest` |
| Интеграционные сценарии | 28 | `IntegrationTest`, `IntegrationWorkOrderTest`, `IntegrationStockTest`, `ParallelWorkOrderTest` |
| JavaFX-регрессия в `test` | 1 | `ClientDoubleClickRegressionTest` |

### UI-тесты (51)

| Класс | Тестов |
|---|---:|
| `ClientViewUITest` | 8 |
| `OrderViewUITest` | 19 |
| `MainAppUITest` | 7 |
| `ThemeSettingsUITest` | 3 |
| `DashboardThemeUITest` | 2 |
| `DashboardLayoutUITest` | 3 |
| `AppointmentVisibilityUITest` | 1 |
| `StockOperationDialogThemeUITest` | 1 |
| `views/AppointmentCalendarUITest` | 4 |
| `views/TableStripingUITest` | 3 |

Матрица требований — в [TRACEABILITY_MATRIX.md](TRACEABILITY_MATRIX.md).

## Покрытие по пакетам

Замер JaCoCo 18.11.2026, Linux; UI-тесты в `test` не входят.

| Пакет | Строк покрыто | Строк всего | Покрытие |
|---|---:|---:|---:|
| `com.autoservice` (корень) | 2 069 | 2 668 | 77,5 % |
| `com.autoservice.model` | 250 | 283 | 88,3 % |
| `com.autoservice.services` | 1 364 | 1 549 | 88,1 % |
| `com.autoservice.utils` | 192 | 616 | 31,2 % |
| `com.autoservice.controllers` | 75 | 312 | 24,0 % |
| `com.autoservice.dialogs` | 320 | 1 985 | 16,1 % |
| `com.autoservice.views` | 154 | 5 394 | 2,9 % |
| **Итого** | **4 424** | **12 807** | **34,5 %** |

Низкие `views`/`dialogs` — ожидаемый результат: интерфейс проверяется
UI-тестами, которые не входят в замер. Пороги выставлены по слоям с реальной
логикой, чтобы общий процент не маскировал деградацию.

## Мутационное тестирование

```bash
./gradlew pitest
# build/reports/pitest/index.html
```

| Метрика | Значение |
|---|---|
| Мутаций сгенерировано | 1 084 |
| Убито | 595 (55 %) |
| Без покрытия | 253 |
| Test strength | 72 % |
| Порог | 70 % |

Мутируются `services.*`, `utils.*`, `Validators`, `DateUtils`, `DataStore`,
`Client`, `WorkOrder`, `SparePart`, `Appointment`. UI-классы и тесты JavaFX
исключены: под миньонами PIT тулкит не поднимается. Подробности и история
базовой линии — в [COVERAGE_AND_MUTATION.md](COVERAGE_AND_MUTATION.md).

## CI

`.github/workflows/ci.yml`:

| Джоба | Что делает | Условие |
|---|---|---|
| `build` | `./gradlew build`, отчёты JaCoCo, артефакты тестов и покрытия | push в `main`/`develop`, PR |
| `ui-test` | Xvfb + `./gradlew uiTest`, артефакты | после успешного `build` |
| `mutation` | `./gradlew pitest`, артефакт отчёта | только `workflow_dispatch` |

## Известные ограничения

1. UI-тесты не входят в `./gradlew build`: запускаются отдельно и в CI идут
   после сборки.
2. Цифры покрытия сняты на Linux/headless; на Windows возможны отличия на
   десятые доли процента.
3. PIT не входит в `build`/`check`: прогон долгий, запускается вручную.
4. Общий процент покрытия (34,5 %) не отражает качество UI-проверок: без
   UI-тестов `views` покрыты на 2,9 %.

## Выводы

- Все 571 юнит/интеграционных и 51 UI-тест проходят, пропущенных и
  отключённых тестов нет.
- Пороги JaCoCo и PIT выполнены.
- Сборка `./gradlew build` зелёная, SpotBugs и Checkstyle без ошибок.
- Версия 1.2.0 готова к релизу после ручной проверки портативной
  Windows-сборки (см. [RELEASE_READINESS.md](RELEASE_READINESS.md)).
