# План UI-тестирования AdminSTO 1.2.0

> **Назначение:** инвентарь и правила запуска JavaFX UI-тестов: что проверяется,
> где лежат проверки, как читать результат.
> **Актуализировано:** 18.11.2026
> **Версия ПО:** 1.2.0

## 1. Область и уровни

UI-тесты проверяют поведение интерфейса, которое не покрывается юнит-тестами:
построение экранов, фильтры, переключение вкладок, календарь, темы и стили
таблиц. Всего — **51 тест в 10 классах**. Они вынесены в отдельную задачу
`uiTest` и в `./gradlew build` не входят.

| Уровень | Задача | Что проверяет |
|---|---|---|
| Юнит/интеграционные | `test` | Модели, БД, `DataStore`, сервисы, контроллеры, диалоги без показа окна |
| UI | `uiTest` | JavaFX-экраны, навигация, календарь, темы, стили таблиц |
| Мутации | `pitest` | Качество юнит-тестов (UI-классы исключены) |

## 2. Окружение и запуск

```bash
# локально, headless Monocle
./gradlew uiTest

# Linux CI
xvfb-run -a ./gradlew uiTest

# отчёт
build/reports/tests/uiTest/index.html
build/test-results/uiTest/
```

Настройки headless заданы в `build.gradle`: `glass.platform=Monocle`,
`monocle.platform=Headless`, `prism.order=sw`, `Platform.setImplicitExit(false)`.
Каждый тестовый класс получает отдельную JVM (`forkEvery = 1`), поэтому
статическое состояние `DataStore` не протекает между классами.

Скриншоты-доказательства части тестов складываются в
`build/ui-test-evidence/` (темы, дашборд, календарь, «зебра»).

## 3. Инвентарь

| Класс | Тестов | Что проверяет |
|---|---:|---|
| `ClientViewUITest` | 8 | Пустая таблица, добавление клиента, поиск по имени/телефону, сброс поиска, активация кнопок, колонки лояльности |
| `OrderViewUITest` | 19 | Таблица заказов, поиск по клиенту/номеру, фильтры статусов и суммы, расширенный фильтр, доступность кнопок, сброс фильтров |
| `MainAppUITest` | 7 | Наличие и порядок всех 8 вкладок, переключение, таблицы клиентов/заказов, вкладка лояльности |
| `ThemeSettingsUITest` | 3 | Применение темы из `config/theme.txt` при старте, сохранение выбора без динамической смены, подсказка о перезапуске |
| `DashboardThemeUITest` | 2 | Светлая тема Primer Light и тёмная Dracula: контраст и читаемость дашборда |
| `DashboardLayoutUITest` | 3 | Вёрстка дашборда в светлой и тёмной темах, пустое состояние низкого остатка |
| `AppointmentVisibilityUITest` | 1 | Запись видна и на дашборде, и на вкладке «Запись» |
| `StockOperationDialogThemeUITest` | 1 | Цвета блока типа операции в диалоге склада соответствуют теме |
| `AppointmentCalendarUITest` | 4 | Недельная сетка со стилями, цвет полосы по статусу заказа, счётчик записей в заголовке дня, месячная сетка с точками статусов и подсветкой «сегодня» |
| `TableStripingUITest` | 3 | Чередование строк, «зебра» в тёмной теме, голубая подсветка выбранной строки |

`BaseUITest` — абстрактная база (0 тестов): инициализация тулкита и БД,
`findNode`, `clickOn`, `writeText`, `selectFromComboBox`, проверки видимости и
текста. `JavaFxTestSupport` используется тестами, которым нужен реальный
тулкит: `initToolkit()`, `runOnFxThread`, `closeStageWhenShown`, таймауты.

## 4. Матрица «требование → UI-тест»

| Требование | Тесты |
|---|---|
| Навигация по 8 вкладкам | `MainAppUITest.mainTabPaneHasAllTabs`, `tabTitlesAreCorrect`, `switchTabByIndex` |
| Поиск и фильтры клиентов | `ClientViewUITest.searchByNameFiltersTable`, `searchByPhoneFiltersTable`, `clearingSearchRestoresAllRows` |
| Поиск и фильтры заказов | `OrderViewUITest.testSearchByClientName`, `testStatusFilterNew`, `testTotalRangeFilter`, `testResetFilters` |
| Календарь «Запись» | `AppointmentCalendarUITest.*`, `AppointmentVisibilityUITest` |
| Темы оформления | `ThemeSettingsUITest.*`, `DashboardThemeUITest.*`, `StockOperationDialogThemeUITest` |
| «Зебра» и подсветка таблиц | `TableStripingUITest.rowsAlternateTheirBackground`, `rowsKeepAlternatingInDarkTheme`, `selectedRowIsHighlightedWithAccentColor` |
| Вёрстка дашборда | `DashboardLayoutUITest.*` |
| Колонки лояльности | `ClientViewUITest.loyaltyColumnsShowClientStatus` |

## 5. Критерии приёмки UI-изменений

1. `./gradlew uiTest` — 51/51, 0 падений.
2. Для изменений тем/таблиц/календаря приложены скриншоты из
   `build/ui-test-evidence/` или ручные снимки на демо-данных.
3. Новая функциональность UI получает отдельный тест в одном из классов выше
   либо новый класс с суффиксом `UITest`.
4. Тест не зависит от реального `config/` и рабочей БД: используются
   `@TempDir`/`initForTest`/подмена пути темы.

## 6. Ограничения

- UI-тесты не входят в `./gradlew build`; в CI это отдельная джоба после
  успешной сборки.
- На машине без дисплея нужен Monocle; в Linux CI дополнительно `xvfb-run`.
- UI-тесты не участвуют в JaCoCo-порогах и PIT.
- Часть проверок опирается на CSS-классы и снимки сцены; при смене разметки
  тест нужно обновлять вместе с экраном.
