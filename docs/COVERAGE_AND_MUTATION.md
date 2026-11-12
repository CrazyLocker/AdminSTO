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
| `com.autoservice.services` | 75% | 87,4% | 87,4% (после герметизации) |
| `com.autoservice.utils` | 29% | 31,2% | 31,2% |
| `com.autoservice.model` | 55% | 61,4% | 61,2% |

Пороги выставлены на 2–3 п.п. ниже **минимума по платформам** (то есть по CI):
они ловят деградацию, но не мешают работе. История роста: 46,2% (до тестов
`ExportService`/`ImportService`/`SettingService`/`ScheduleService`) → 72,2%
(2026-11-12, порог 60%) → 87,4% (после тестов `BackupService` и герметичного
`TableStateManagerTest`, порог 75%, требование «покрытие services выше 75%»).

**Почему раньше Linux был ниже.** Разница сидела в `services/TableStateManager`
(46,8% на Windows против 6,4% на Linux): старый тест писал состояние в реальный
каталог `config/table-state`, которого в чистом чекауте CI нет. Теперь каталог
подменяется на `@TempDir` через `TableStateManager.setStorageDirForTest(...)`,
поэтому цифры платформ совпадают (94% по классу на обеих).

### Общая картина (строки; Windows / Linux-CI, сессия 2026-11-12)

| Пакет | Покрытие (Win / Linux) | Строк |
|-------|----------|-------|
| `com.autoservice` (корень) | 60,7% | 2646 |
| `com.autoservice.model` | 61,4% | 277 |
| `com.autoservice.services` | 87,4% | 1544 |
| `com.autoservice.utils` | 31,2% | 616 |
| `com.autoservice.controllers` | 18,0% | 316 |
| `com.autoservice.dialogs` | 13,8% | 1979 |
| `com.autoservice.views` | 2,7% | 5207 |
| **Итого по проекту** | **30,1% строк, 28,6% инструкций, 23,8% ветвей** | 12585 строк |

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

### Базовая линия (2026-11-12, после тестов `BackupService` / `TableStateManager`)

```
Мутаций сгенерировано: 1054
Убито:                 489 (46% от всех)
Без покрытия:          386
Test strength:         73%  (killed / (generated − NO_COVERAGE))
Покрытие мутируемых классов: 68% строк
Время прогона:         59 с
```

История: линия 2026-11-11 — убито 325 (31%), без покрытия 622, test strength 75%,
покрытие мутируемых классов 48%. Линия после тестов импорта/экспорта/настроек —
439 (42%), 451, 73%, 62%. Рост общего mutation score 31% → 42% → 46% дали сначала
`ExportService` (было 0% убитых мутаций) и `ImportService`, затем `BackupService`
(было 29% убитых → 78%, без покрытия 77 → 13).

Гейт держится по **test strength** (`testStrengthThreshold = 70` при факте 73%), а не по общему mutation score 46%: общий счёт падает от любого нового непокрытого класса, хотя качество тестов не изменилось. Порог — на 3 п.п. ниже факта, чтобы ловить деградацию. `mutationThreshold` выключен (0).

### Где тесты слабее всего (NO_COVERAGE → 0% score)

| Класс | Мутаций | Без покрытия | Score |
|-------|---------|--------------|-------|
| `DataStore` | 159 | 98 | 54% |
| `IconHelper` | 59 | 59 | 0% |
| `ServicePartHelper` | 43 | 43 | 0% |
| `SparePart` | 34 | 34 | 0% |
| `ValidationErrorIndicator` | 31 | 31 | 0% |
| `ValidationUtils` | 62 | 28 | 100% |
| `Formatters` | 45 | 28 | 100% |
| `LoyaltyService` | 121 | 15 | 74% |
| `BackupService` | 112 | 13 | 78% |
| `ImportService` | 129 | 9 | 74% |
| `DateUtils` | 58 | 5 | 79% |

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
