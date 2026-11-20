# Покрытие и мутационное тестирование

> **Назначение:** как измеряется качество тестов, какие пороги действуют, что
> осталось вне измерения.
> **Актуализировано:** 18.11.2026
> **Версия ПО:** 1.2.0

Документ фиксирует, как в проекте измеряется качество тестов, какие пороги
действуют и что осталось вне измерения. Обновляйте базовую линию при
существенных сдвигах, решения об исключениях — только осознанно.

## 1. Покрытие кода (JaCoCo)

```bash
./gradlew jacocoTestReport            # отчёт: build/reports/jacoco/test/html/index.html
./gradlew jacocoTestCoverageVerification   # проверка порогов
./gradlew build                       # пороги проверяются автоматически (check зависит от них)
```

Отчёт в трёх форматах: HTML (для человека), XML и CSV (для инструментов и CI).

Покрытие считается по задаче `test`; UI-тесты (`uiTest`) в него не входят.

### Пороги (`jacocoTestCoverageVerification`)

| Пакет | Порог (строки) | Факт (Linux, 18.11.2026) | Запас |
|-------|----------------|--------------------------|-------|
| `com.autoservice.services` | 75% | 88,1% | 13,1 п.п. |
| `com.autoservice.utils` | 29% | 31,2% | 2,2 п.п. |
| `com.autoservice.model` | 55% | 88,3% | 33,3 п.п. |

Пороги выставлены на 2–3 п.п. ниже фактического уровня: они ловят деградацию,
но не мешают работе. История роста `services`: 46,2% (до тестов
`ExportService`/`ImportService`/`SettingService`/`ScheduleService`) → 72,2%
(2026-11-12, порог 60%) → 87,4% (после тестов `BackupService` и герметичного
`TableStateManagerTest`, порог 75%). На 18.11.2026 замер даёт 88,1% на Linux;
цифры Windows могут отличаться на десятые доли.

**Почему раньше Linux был ниже.** Разница сидела в `services/TableStateManager`
(46,8% на Windows против 6,4% на Linux): старый тест писал состояние в реальный
каталог `config/table-state`, которого в чистом чекауте CI нет. Теперь каталог
подменяется на `@TempDir` через `TableStateManager.setStorageDirForTest(...)`,
поэтому цифры платформ совпадают (94% по классу на обеих).

### Тесты, которым нужен JavaFX

Четыре теста, ранее помеченные `@Disabled` (`ClientControllerTest.testEditClient`,
`OrderControllerTest.testRefreshTable/testViewOrder/testChangeStatus`), включены:
тулкит поднимается через `com.autoservice.support.JavaFxTestSupport.initToolkit()`
(headless Monocle + **`Platform.setImplicitExit(false)`** — без него закрытие
последнего окна останавливает тулкит и следующие тесты впадают в таймаут).
Диалоги закрываются фоновым «кликером» `closeStageWhenShown(title, timeout)`.
Итог: **571 тест, 0 пропущенных, 0 `@Disabled`**.

### UI-тесты

51 UI-тест в 10 классах вынесен в отдельную задачу `uiTest` и в замер JaCoCo не
входит. Календарь и таблицы проверяют `views/AppointmentCalendarUITest` (4 теста:
недельная сетка, полоса статуса, счётчик дня, месячная сетка) и
`views/TableStripingUITest` (3 теста: «зебра», тёмная тема, подсветка выбора).
Темы — `ThemeSettingsUITest`, `DashboardThemeUITest`,
`StockOperationDialogThemeUITest`. Полный перечень классов — в
[TEST_REPORT.md](TEST_REPORT.md).

### Общая картина (строки; замер Linux, 18.11.2026)

| Пакет | Покрытие строк | Строк |
|-------|----------------|-------|
| `com.autoservice` (корень) | 77,5% | 2 668 |
| `com.autoservice.model` | 88,3% | 283 |
| `com.autoservice.services` | 88,1% | 1 549 |
| `com.autoservice.utils` | 31,2% | 616 |
| `com.autoservice.controllers` | 24,0% | 312 |
| `com.autoservice.dialogs` | 16,1% | 1 985 |
| `com.autoservice.views` | 2,9% | 5 394 |
| **Итого по проекту** | **34,5% строк, 32,2% инструкций, 27,4% ветвей** | **12 807 строк** |

Низкие `views`/`dialogs` ожидаемы: их покрывают UI-тесты (`uiTest`), которые не
участвуют в замере. Если понадобится общая цифра «с UI», считайте два прогона
(`test` и `uiTest`) и объединяйте данные.

## 2. Мутационное тестирование (PIT)

```bash
./gradlew pitest
```

Отчёты: `build/reports/pitest/index.html`, `build/reports/pitest/mutations.xml`.
В `build`/`check` не входит: прогон долгий, запускается вручную или отдельной
джобой CI (`Actions → CI → Run workflow`, джоба «Mutation testing (PIT, manual)»).

### Версии (проверены 2026-11-11)

| Компонент | Версия | Источник |
|-----------|--------|----------|
| `info.solidsoft.pitest` (плагин) | 1.19.0 | Plugin Portal |
| `pitestVersion` | 1.30.2 | Maven Central |
| `junit5PluginVersion` | 1.2.3 | Maven Central |

Окружение: Gradle 8.5, JDK 21 (Temurin), JUnit Platform 1.10.1.

### Что мутируется

`targetClasses`: `services.*`, `utils.*`, `Validators`, `DateUtils`, `DataStore`,
`Client`, `WorkOrder`, `SparePart`, `Appointment` — бизнес-логика.

Исключено (осознанные решения, см. раздел «Почему так»):

* интерфейс JavaFX — `views.*`, `dialogs.*`, `App` (проверяется UI-тестами);
* геттеры/сеттеры и `Object`-методы — `excludedMethods = equals, hashCode,
  toString, get*, set*, is*`;
* конфигурация и состояния окон — `WindowStateManager`, `TableStateManager`,
  `ScheduleService`, `ThemeManager`, `ThemePreferences`;
* тесты JavaFX в мутационном прогоне не участвуют — под миньонами PIT тулкит не
  поднимается: `excludedTestClasses = *UITest, BaseUITest,
  views.ClientDoubleClickRegressionTest`.

Для разовых исключений из кода есть маркер `@DoNotMutate`
(`src/main/java/com/autoservice/DoNotMutate.java`) — PIT распознаёт его без
дополнительных настроек.

### Базовая линия (18.11.2026, прогон `./gradlew pitest`)

```
Мутаций сгенерировано: 1084
Убито:                 595 (55% от всех)
Без покрытия:          253
Test strength:         72%  (killed / (generated − NO_COVERAGE))
Покрытие мутируемых классов: 78% строк
Время прогона:         74 с
```

История: линия 2026-11-11 — убито 325 (31%), без покрытия 622, test strength 75%,
покрытие мутируемых классов 48%. Линия после тестов импорта/экспорта/настроек —
439 (42%), 451, 73%, 62%. Линия после тестов `BackupService` / `TableStateManager` —
489 (46%), 386, 73%, 68%. Рост общего mutation score 31% → 42% → 46% → 55% дали
сначала `ExportService` (было 0% убитых мутаций) и `ImportService`, затем
`BackupService` (было 29% убитых → 78%, без покрытия 77 → 13), затем тесты
`DataStore` (`DataStorePersistenceTest` — семантика `save()`, `DataStoreRelationsTest` —
связи услуга-запчасть/списки/автомобили/ТО/настройки: без покрытия 98 → 9,
score 54% → 60%) и `SparePartTest` (`SparePart`: без покрытия 34 → 0, score 0% → 82%).

Гейт держится по **test strength** (`testStrengthThreshold = 70` при факте 72%), а не по общему mutation score 55%: общий счёт падает от любого нового непокрытого класса, хотя качество тестов не изменилось. Порог — на 2 п.п. ниже факта, чтобы ловить деградацию. `mutationThreshold` выключен (0).

### Где тесты слабее всего (NO_COVERAGE → 0% score)

| Класс | Мутаций | Без покрытия | Score |
|-------|---------|--------------|-------|
| `IconHelper` | 59 | 59 | 0% |
| `ServicePartHelper` | 43 | 43 | 0% |
| `ValidationErrorIndicator` | 31 | 31 | 0% |
| `TooltipHelper` | 5 | 5 | 0% |
| `ValidationUtils` | 62 | 28 | 55% |
| `Formatters` | 45 | 20 | 38% |
| `LoyaltyService` | 121 | 15 | 64% |
| `BackupService` | 112 | 13 | 70% |
| `DataStore` | 189 | 9 | 61% |
| `ImportService` | 129 | 9 | 75% |
| `JsonStateManager` | 23 | 5 | 50% |
| `AutoAddSparePartService` | 17 | 5 | 92% |
| `DateUtils` | 58 | 3 | 80% |

Первый кандидат на доработку — `IconHelper`, `ServicePartHelper`,
`ValidationErrorIndicator`, `TooltipHelper` (0% при заметном объёме логики),
затем `DataStore` (9 непокрытых мутаций при 189 сгенерированных).

## 3. Почему так

* Порог держим по слоям, а не по проекту: общий процент размывается UI-классами,
  которые в задачу `test` не входят, и превращается в число, которое никто не
  читает.
* Геттеры/сеттеры и конфигурация исключены из мутаций: их «убийство» не
  доказывает поведение, а отчёт заполняет шумом.
* PIT не подключён к `check`: полный прогон на каждый push замедляет обратную
  связь сильнее, чем приносит пользы; ручной запуск в CI + порог 70% закрывают
  потребность.
* Тесты, которым нужна файловая система, пишут только в `@TempDir` (или в каталог,
  подставленный через тестовый шов — `TableStateManager.setStorageDirForTest`,
  `BackupService.createBackup(sourceDb, backupDir, tableStateDir, windowStateDir)`).
  Иначе результат зависит от файлов, оставшихся на машине разработчика, и CI
  показывает другие цифры — именно так случилось с `TableStateManager` (6,4% против
  46,8%).
