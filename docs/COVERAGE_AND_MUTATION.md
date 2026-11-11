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

| Пакет | Порог (строки) | Факт на 2026-11-11 |
|-------|----------------|--------------------|
| `com.autoservice.services` | 44% | 46,2% |
| `com.autoservice.utils` | 29% | 31,2% |
| `com.autoservice.model` | 55% | 58,8% |

Пороги выставлены на 2–3 п.п. ниже фактического уровня: они ловят деградацию, но
не мешают работе. Если покрытие выросло — поднимите порог, это дешёвая защита.

### Общая картина (строки, сессия 2026-11-11)

| Пакет | Покрытие | Строк |
|-------|----------|-------|
| `com.autoservice` (корень) | 58,9% | 2646 |
| `com.autoservice.model` | 58,8% | 277 |
| `com.autoservice.services` | 46,2% | 1529 |
| `com.autoservice.utils` | 31,2% | 616 |
| `com.autoservice.controllers` | 18,0% | 316 |
| `com.autoservice.dialogs` | 13,8% | 1979 |
| `com.autoservice.views` | 2,7% | 5207 |
| **Итого по проекту** | **~23% инструкций / ~18% строк** | |

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

### Базовая линия (2026-11-11)

```
Мутаций сгенерировано: 1054
Убито:                 325 (31% от всех)
Без покрытия:          622
Test strength:         75%  (killed / (generated − NO_COVERAGE))
Покрытие мутируемых классов: 48% строк
Время прогона:         44 с
```

`mutationThreshold = 70` — на 5 п.п. ниже факта, чтобы ловить деградацию.

### Где тесты слабее всего (NO_COVERAGE → 0% score)

| Класс | Мутаций | Без покрытия | Score |
|-------|---------|--------------|-------|
| `DataStore` | 159 | 111 | 65% |
| `ImportService` | 130 | 92 | 61% |
| `BackupService` | 113 | 77 | 81% |
| `ExportService` | 59 | 59 | 0% |
| `IconHelper` | 59 | 59 | 0% |
| `ServicePartHelper` | 43 | 43 | 0% |
| `SparePart` | 34 | 34 | 0% |
| `ValidationErrorIndicator` | 31 | 31 | 0% |
| `LoyaltyService` | 121 | 15 | 74% |
| `ValidationUtils` | 62 | 28 | 100% |
| `DateUtils` | 58 | 5 | 79% |
| `Formatters` | 45 | 28 | 100% |

Первый кандидат на доработку — `ExportService` и `SparePart` (0% при заметном
объёме логики), затем непокрытые ветви `DataStore` и `ImportService`.

## 3. Почему так

* Порог держим по слоям, а не по проекту: общий процент размывается UI-классами,
  которые в задачу `test` не входят, и превращается в число, которое никто не
  читает.
* Геттеры/сеттеры и конфигурация исключены из мутаций: их «убийство» не
  доказывает поведение, а отчёт заполняет шумом.
* PIT не подключён к `check`: полный прогон на каждый push замедляет обратную
  связь сильнее, чем приносит пользы; ручной запуск в CI + порог 70% закрывают
  потребность.
