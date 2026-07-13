# Инструкция: Добавление сохранения настроек таблицы (ширина, порядок, сортировка)

## Архитектура системы

Система состоит из 4 уровней:

```
App.java                          — точка сохранения при закрытии
  ↓
View (ClientView)                 — создание таблицы, загрузка состояния
  ↓
Controller (ClientController)     — обновление данных (не должен ломать SortedList)
  ↓
TableStateManager                 — сохранение/восстановление в JSON
  ↓
Models: TableState, ColumnState, SortState — структуры данных для JSON
```

**Хранилище:** `config/table-state/{tableId}.json` — один файл на таблицу.

---

## Часть 1. Модели данных (уже созданы, трогать не нужно)

### `TableState.java` — состояние всей таблицы
| Поле | Тип | Назначение |
|------|-----|------------|
| `tableId` | String | Идентификатор таблицы (например `"clientTable"`) |
| `version` | String | Версия формата (`"1.0"`) |
| `timestamp` | String | Время сохранения (ISO-8601) |
| `columns` | List\<ColumnState\> | Состояние каждой колонки |
| `sortOrder` | List\<SortState\> | Порядок сортировки |

### `ColumnState.java` — состояние одной колонки
| Поле | Тип | Назначение |
|------|-----|------------|
| `id` | String | ID колонки (устанавливается через `setId()`) |
| `width` | double | Ширина в пикселях |
| `visible` | boolean | Видимость колонки |
| `index` | Integer | Порядковый номер в таблице (0, 1, 2…) |

### `SortState.java` — состояние сортировки
| Поле | Тип | Назначение |
|------|-----|------------|
| `columnId` | String | ID колонки, по которой сортируем |
| `order` | String | `"ASC"` или `"DESC"` |

---

## Часть 2. TableStateManager — методы и логика

Все методы **статические**, работают через reflection (принимают `Object table`), поэтому совместимы с любым `TableView<?>`.

### `saveTableState(Object table, String tableId)`
**Что делает:** читает текущее состояние таблицы и пишет в JSON.

**Логика:**
1. Создаёт директорию `config/table-state/` если её нет
2. Перебирает `table.getColumns()` — для каждой колонки сохраняет:
   - **`getId()`** → `id` — идентификатор колонки
   - **`getWidth()`** → `width` — **именно `getWidth()`, не `getPrefWidth()`!** Когда пользователь тянет колонку мышкой, JavaFX обновляет `width`, но `prefWidth` остаётся прежним
   - **`isVisible()`** → `visible`
   - **индекс в списке** `i` → `index` — порядок отображения
3. Читает `table.getSortOrder()` — список `TableColumn`, для каждого:
   - `getId()` → `columnId`
   - `getSortType()` → `"ASC"` если `ASCENDING`, иначе `"DESC"`
4. Сериализует через Gson в файл `{tableId}.json`

### `loadTableState(Object table, String tableId)`
**Что делает:** читает JSON и восстанавливает состояние.

**Логика:**
1. Если файл не существует — выход (первый запуск)
2. Десериализует JSON в `TableState`
3. Вызывает `restoreColumnStates()` — порядок + ширина + видимость
4. Вызывает `restoreSortOrder()` — сортировка

### `restoreColumnStates(Object table, TableState tableState)` — приватный
**Самый сложный метод. Порядок операций критичен.**

**Логика (4 шага):**

**ШАГ 1 — Сборка нового порядка (БЕЗ установки ширины):**
- Строит `Map<String, Object> columnMap` — текущие колонки по ID
- Сортирует колонки из JSON по полю `index`
- Собирает `List<Object> newOrder` — колонки в порядке из JSON
- Колонки, отсутствующие в JSON, добавляются в конец
- Ширины и видимость временно складываются в `widthMap` и `visibleMap`

**ШАГ 2 — Применение порядка:**
- `currentColumns.setAll(newOrder)` — меняет порядок колонок
- **Важно:** ширины НЕ устанавливаются здесь! `setAll()` триггерит layout pass, который сбросит `prefWidth`

**ШАГ 3 — Установка ширины и видимости ПОСЛЕ `setAll()`:**
- Для каждой колонки вызывает **одновременно**:
  - `setPrefWidth(width)` — желаемая ширина
  - `setMinWidth(width)` — фиксация минимума
  - `setMaxWidth(width)` — фиксация максимума
- **Зачем min/max:** JavaFX layout pass может проигнорировать `prefWidth`, но если min=max=pref, ширина зафиксирована жёстко
- `setVisible(visible)` — видимость

**ШАГ 4 — Снятие фиксации (через `Platform.runLater`):**
- В следующем tick'е сбрасывает `setMinWidth(0)` и `setMaxWidth(Double.MAX_VALUE)`
- **Зачем:** иначе пользователь не сможет менять ширину мышкой (колонка заблокирована)

### `restoreSortOrder(Object table, TableState tableState)` — приватный
**Логика:**
1. Очищает `table.getSortOrder()`
2. Для каждого `SortState` из JSON:
   - Находит колонку по ID
   - Устанавливает `setSortType(ASCENDING/DESCENDING)` через reflection
   - Добавляет колонку в `sortOrder` таблицы
3. **Важно:** TableView автоматически применяет сортировку, только если данные обёрнуты в `SortedList` (см. Часть 3)

### `resetTableState(String tableId)` — удаляет JSON-файл
### `hasSavedState(String tableId)` — проверяет наличие файла

---

## Часть 3. Интеграция во View (на примере ClientView)

### 3.1. Поля класса — 3 списка вместо 1

```java
// НЕДОСТАТОЧНО: только FilteredList — сортировка работать не будет
private static FilteredList<Client> filteredClients;

// ПРАВИЛЬНО: FilteredList → SortedList → TableView
private static FilteredList<Client> filteredClients;
private static SortedList<Client> sortedClients;
private static ObservableList<Client> masterData;
```

**Почему:** `FilteredList` фильтрует данные, но не сортирует. `SortedList` связывает компаратор TableView с данными. Без `SortedList` клик по заголовку колонки меняет `sortOrder`, но данные не пересортировываются.

### 3.2. Создание колонок — каждой нужен ID

```java
TableColumn<Client, String> colLastName = new TableColumn<>("Фамилия");
colLastName.setId("colLastName");              // ОБЯЗАТЕЛЬНО! Без ID сохранение не работает
colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
colLastName.setPrefWidth(130);                 // Ширина по умолчанию
colLastName.setSortable(true);                 // Включаем сортировку
```

**ID должен быть уникальным в пределах таблицы.** По нему `TableStateManager` сопоставляет колонку в коде с колонкой в JSON.

### 3.3. Resize policy — ОБЯЗАТЕЛЬНО UNCONSTRAINED

```java
table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
```

**Почему:** `CONSTRAINED_RESIZE_POLICY` (по умолчанию) принудительно растягивает колонки на всю ширину таблицы, сбрасывая `prefWidth` при каждом layout pass. `UNCONSTRAINED` позволяет колонкам сохранять заданную ширину.

### 3.4. Связывание SortedList с компаратором таблицы

```java
filteredClients = new FilteredList<>(FXCollections.observableArrayList(DataStore.getClients()), p -> true);

// КЛЮЧЕВАЯ СТРОКА: без неё сортировка не работает
sortedClients = new SortedList<>(filteredClients);
sortedClients.comparatorProperty().bind(table.comparatorProperty());
table.setItems(sortedClients);
```

**Логика:** `comparatorProperty().bind()` связывает компаратор TableView (который меняется при клике на заголовок) с компаратором SortedList. SortedList пересортировывает данные автоматически.

### 3.5. Загрузка состояния — через Platform.runLater

```java
// В методе create(), ПОСЛЕ добавления таблицы в контейнер:
mainContainer.getChildren().addAll(titleLabel, topPanel, clientTable);
refreshClientList();

// ЗАГРУЗКА — отложена до после отрисовки
Platform.runLater(() -> {
    if (clientTable != null) {
        TableStateManager.loadTableState(clientTable, "clientTable");
    }
});
```

**Почему `Platform.runLater`:** если вызвать `loadTableState` синхронно, таблица ещё не отрисована. `setAll()` и `setPrefWidth()` применятся, но первый layout pass JavaFX **сбросит** их. `Platform.runLater` гарантирует, что состояние применяется **после** первого layout pass.

### 3.6. Метод refresh — пересоздаёт SortedList

```java
public static void refreshClientList() {
    masterData = FXCollections.observableArrayList(DataStore.getClients());
    filteredClients = new FilteredList<>(masterData, p -> true);
    
    if (clientTable != null) {
        // Пересоздаём SortedList — иначе новый filteredClients не подхватится
        sortedClients = new SortedList<>(filteredClients);
        sortedClients.comparatorProperty().bind(clientTable.comparatorProperty());
        clientTable.setItems(sortedClients);
    }
    
    if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
        filterClients(searchField.getText());
    }
}
```

**Важно:** при refresh нужно **пересоздать** `SortedList` с новым `FilteredList` и **заново** сделать `bind()`. Если просто поменять данные в `masterData`, `SortedList` может не подхватить изменения.

### 3.7. Getter для таблицы

```java
public static TableView<Client> getTable() {
    return clientTable;
}
```

Нужен, чтобы `App.java` мог вызвать `saveTableState` при закрытии.

---

## Часть 4. Контроллер — НЕ вызывать setItems напрямую

```java
public static void refreshTable() {
    // ТАК НЕЛЬЗЯ — ломает SortedList:
    // clientTable.setItems(FXCollections.observableArrayList(DataStore.getClients()));
    
    // ПРАВИЛЬНО — делегируем во View, который пересоздаст SortedList:
    ClientView.refreshClientList();
}
```

**Почему:** прямой `setItems()` заменяет `SortedList` обычным `ObservableList`. После этого сортировка перестаёт работать, а `comparatorProperty().bind()` остаётся привязан к старому (уже неактивному) `SortedList`.

---

## Часть 5. App.java — сохранение при закрытии

```java
primaryStage.setOnCloseRequest(e -> {
    // Сохраняем состояние КАЖДОЙ таблицы
    TableStateManager.saveTableState(ClientView.getTable(), "clientTable");
    TableStateManager.saveTableState(OrderView.getTable(), "orderTable");
    // ... другие таблицы ...
    
    DataStore.save();
    Database.close();
    // ...
});
```

**Важно:** `saveTableState` использует `getWidth()` — фактическую ширину после отрисовки. Поэтому сохранение должно происходить **при закрытии**, когда таблица уже отрисована и пользователь мог изменить колонки.

---

## Часть 6. Чек-лист для добавления в новую таблицу

| # | Что сделать | Где | 
|---|-------------|-----|
| 1 | Добавить поля `filteredXxx`, `sortedXxx`, `masterData` | View |
| 2 | Каждой колонке задать `setId("colXxx")` | View |
| 3 | Установить `UNCONSTRAINED_RESIZE_POLICY` | View |
| 4 | Создать `FilteredList` → `SortedList` → `bind(comparatorProperty)` → `setItems` | View |
| 5 | В `create()` вызвать `loadTableState` через `Platform.runLater` | View |
| 6 | В `refreshXxxList()` пересоздавать `SortedList` и заново `bind` | View |
| 7 | В контроллере `refreshTable()` вызывать `View.refreshXxxList()`, **не** `setItems` | Controller |
| 8 | Добавить `getTable()` getter | View |
| 9 | Добавить `saveTableState(XxxView.getTable(), "xxxTable")` в `setOnCloseRequest` | App.java |

---

## Часть 7. Подводные камни

| Проблема | Причина | Решение |
|----------|---------|---------|
| Ширина не восстанавливается | `setPrefWidth` сбрасывается layout pass после `setAll()` | Фиксировать через `setMinWidth`+`setMaxWidth`, снимать в `Platform.runLater` |
| Сортировка не работает | Нет `SortedList`, данные напрямую в `FilteredList` | Обернуть в `SortedList`, `bind(comparatorProperty)` |
| Порядок сбрасывается при refresh | Контроллер вызывает `setItems` напрямую | Делегировать в `View.refreshXxxList()` |
| Сохраняется неверная ширина | Использовался `getPrefWidth()` вместо `getWidth()` | Только `getWidth()` — он отражает изменения мышкой |
| Колонки растягиваются на всю ширину | `CONSTRAINED_RESIZE_POLICY` | `UNCONSTRAINED_RESIZE_POLICY` |
| После восстановления нельзя менять ширину | Не сняты `minWidth`/`maxWidth` | Снимать в `Platform.runLater` |
| Состояние не загружается при первом запуске | Нет JSON-файла | Это нормально — `loadTableState` просто выходит |
