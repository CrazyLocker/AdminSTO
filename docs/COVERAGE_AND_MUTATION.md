# Покрытие и мутационное тестирование

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

| Пакет | Порог (строки) | Факт: Windows | Факт: Linux (CI) |
|-------|----------------|---------------|------------------|
| `com.autoservice.services` | 75% | 87,4% | 87,7% (CI 43554482517) |
| `com.autoservice.utils` | 29% | 31,2% | 31,2% |
| `com.autoservice.model` | 55% | 88,3% | 88,1% |

Пороги выставлены на 2–3 п.п. ниже **минимума по платформам** (то есть по CI):
они ловят деградацию, но не мешают работе. История роста: 46,2% (до тестов
`ExportService`/`ImportService`/`SettingService`/`ScheduleService`) → 72,2%
(2026-11-12, порог 60%) → 87,4% (после тестов `BackupService` и герметичного
`TableStateManagerTest`, порог 75%, требование «покрытие services выше 75%»).
Прогон CI 43554482517 подтвердил 87,7% на Linux — запас над порогом 12 п.п.

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
Итог: 568 тестов, 0 пропущенных.

### Общая картина (строки; Windows / Linux-CI, сессия 2026-11-13)

| Пакет | Покрытие (Win / Linux) | Строк |
|-------|----------|-------|
| `com.autoservice` (корень) | 77,5% / 76,8% | 2659 |
| `com.autoservice.model` | 88,3% / 88,1% | 283 |
| `com.autoservice.services` | 87,4% / 87,7% | 1544 |
| `com.autoservice.utils` | 31,2% / 31,2% | 616 |
| `com.autoservice.controllers` | 23,7% / 23,7% | 316 |
| `com.autoservice.dialogs` | 16,2% / 16,2% | 1979 |
| `com.autoservice.views` | 2,9% / 2,9% | 5207 |
| **Итого по проекту** | **34,9% / 34,8% строк, 32,4% инструкций, 27,2% ветвей** | 12604 строк |

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

### Базовая линия (2026-11-13, после тестов `DataStore` / `SparePart`)

```
Мутаций сгенерировано: 1081
Убито:                 590 (55% от всех)
Без покрытия:          255
Test strength:         71%  (killed / (generated − NO_COVERAGE))
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

Гейт держится по **test strength** (`testStrengthThreshold = 70` при факте 71%), а не по общему mutation score 55%: общий счёт падает от любого нового непокрытого класса, хотя качество тестов не изменилось. Порог — на 1 п.п. ниже факта, чтобы ловить деградацию. `mutationThreshold` выключен (0).

### Где тесты слабее всего (NO_COVERAGE → 0% score)

| Класс | Мутаций | Без покрытия | Score |
|-------|---------|--------------|-------|
| `IconHelper` | 59 | 59 | 0% |
| `ServicePartHelper` | 43 | 43 | 0% |
| `ValidationErrorIndicator` | 31 | 31 | 0% |
| `ValidationUtils` | 62 | 28 | 55% |
| `Formatters` | 45 | 20 | 38% |
| `DataStore` | 159 | 9 | 60% |
| `ImportService` | 129 | 9 | 70% |
| `BackupService` | 112 | 13 | 70% |
| `LoyaltyService` | 121 | 15 | 64% |
| `DateUtils` | 58 | 5 | 72% |
| `SparePart` | 34 | 0 | 82% |

Первый кандидат на доработку — `IconHelper`, `ServicePartHelper`, `SparePart`,
`ValidationErrorIndicator` (0% при заметном объёме логики), затем `DataStore`
(98 непокрытых мутаций — самый крупный резерв).

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
