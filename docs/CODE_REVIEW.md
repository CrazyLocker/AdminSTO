# Техническое ревью проекта AdminSTO

**Ревизия:** `07fe2b0e36f79e4ff126fefad57f99855ebd42e3` (`main`, 06.11.2026)
**Задача:** [#24](https://github.com/CrazyLocker/AdminSTO/issues/24) — техническое ревью проекта без изменения кода, отчёт в виде Markdown-файла.
**Объём:** 126 Java-файлов / 32 825 строк: прод-код — 83 файла / 24 472 строки (`src/main/java`), тесты — 43 файла / 8 353 строки (`src/test/java`).

## Методика и ограничения

Ревью статическое, каждый пункт проверен по исходникам:

- структура и методы разобраны скриптом (границы методов — по балансу скобок, длина — от строки сигнатуры до закрывающей скобки);
- дублирование: нормализованное сравнение тел методов, построчное сравнение файлов (`difflib`), поиск одинаковых блоков (окно 25 строк) между файлами; каждый крупный случай дополнительно сверен через `diff`;
- ресурсы: найдены все точки `new FileInputStream/FileOutputStream/FileReader/FileWriter`, `Files.walk/list/lines`, `getConnection()`, `createStatement()/prepareStatement()`, `PdfWriter/PdfDocument/Document` и проверено, попадают ли они в `try-with-resources` или закрываются явно;
- NPE: проверены разыменования результатов lookup-методов (`*ById`, `getSettingByKey`), nullable-полей моделей, распаковки `Integer`/`Double`, `Spinner.getValue()`, ячейки таблиц и `updateItem`.

**Ограничение:** сборка, тесты и SpotBugs/Checkstyle **не запускались** — в среде ревью нет JDK (`java`/`javac` отсутствуют, каталог `~/.gradle/jdks` пуст). Поведенческие утверждения (например, про Windows и открытые файлы) сделаны по коду и схеме БД; для NPE-рисков указано условие срабатывания. Номера строк относятся к указанной ревизии.

## Резюме

| Категория | Критичные | Рекомендации |
|---|---:|---:|
| 1. Дублирование кода | — | 12 |
| 2. Потенциальные утечки ресурсов | 3 | 3 |
| 3. Потенциальные `NullPointerException` | 1 (кластер) | 4 |
| 4. Методы длиннее 50 строк | — | 84 метода, приоритеты ниже |

Код приложения не изменялся — добавлен только этот файл.

**Сильные стороны (проверено, а не «для баланса»):**

- работа с JDBC в основном обёрнута в `try-with-resources`: 75 `try (Connection …)`, 194 `try-with-resources` всего; `DriverManager` не используется, пул Hikari централизован в `AbstractDatabase`;
- ни в одном из 32 обращений к lookup-методам `*ById` в прод-коде не найдено разыменования без проверки на `null`;
- из 41 получения выбранного элемента таблицы/списка только 3 без проверки, и там выбор предустановлен `selectFirst()`;
- во всех кастомных ячейках `updateItem` проверяются `empty`/`item == null`;
- `e.printStackTrace()` отсутствует, пустых `catch` нет, TODO/FIXME — 4.

## Структура пакетов

Прод-код — 83 файла / 24 472 строки. Тесты — 43 файла / 8 353 строки (`src/test/java`, включая `testutils` и `builders`).

| Пакет | Файлов | Строк | Что находится |
|---|---:|---:|---|
| `com.autoservice` | 18 | 6 696 | доменные модели, слой БД (`AbstractDatabase`, `SQLiteDatabase`, `H2Database`, `DatabaseInterface`, `DatabaseFactory`, `Database`), `DataStore`, `App`, `DateUtils`, `Validators` |
| `com.autoservice.views` | 13 | 7 674 | экраны JavaFX (`SettingsView`, `AppointmentView`, `OrderView`, …) |
| `com.autoservice.dialogs` | 14 | 4 106 | модальные диалоги (создание/редактирование заказа и клиента, импорт/экспорт, печать PDF) |
| `com.autoservice.services` | 11 | 2 883 | импорт/экспорт, бэкапы, отчёты, настройки, состояние таблиц и окон, планировщик |
| `com.autoservice.utils` | 11 | 1 692 | иконки, валидация, обработка ошибок, вспомогательные хелперы |
| `com.autoservice.model` | 10 | 750 | DTO и структуры состояния (связи услуг-запчастей, настройки, состояние таблиц/окон) |
| `com.autoservice.controllers` | 6 | 671 | тонкие контроллеры между view и `DataStore` |

Наблюдения по структуре:

- **корневой пакет перегружен:** из 18 файлов 6 — слой БД (`AbstractDatabase` 1 826, `SQLiteDatabase` 1 059, `H2Database` 641, `DatabaseInterface` 421, `Database` 486, `DatabaseFactory`) — это ~4 400 строк, две трети пакета. Слой БД логично вынести в отдельный пакет (например, `com.autoservice.db`) и разделить интерфейс/реализации/миграции;
- **`views` — самый крупный пакет (31% прод-кода)**, причём `SettingsView` — 2 597 строк (треть пакета) и 17 методов длиннее 50 строк; `AppointmentView` — 1 145 строк. Экраны содержат бизнес-логику (расчёт остатков, создание записей в календарь), которая должна жить в `services`;
- **слой контроллеров почти не используется:** 6 классов на 671 строку — в основном тонкие `refreshTable()`-обёртки, тогда как view-классы обращаются к `DataStore`/`Database` напрямую (`OrderView`, `ClientView`, `SparePartPanel`, …). Это главная причина, по которой дублирование UI (п. 1.1–1.7) так разрастается;
- `services` и `utils` (22 файла) частично дублируют друг друга и содержат мёртвый код (`OilHelper`, `ReportGenerator`, `StatisticsService`, `LoadingIndicator`);
- `model` — простые DTO (750 строк), часть уместно заменить `record`.

## 1. Дублирование кода

### Критичные

Не выявлено: дублирование в этом проекте влияет на стоимость сопровождения, а не на работоспособность. Единственное исключение — расхождение двух описаний схемы БД (п. 1.3), которое уже привело к дрейфу схем.

### Рекомендации

**1.1. Три почти идентичных диалога экспорта — 555 строк, две копии избыточны (~370 строк).**
`ExportClientsDialog.java`, `ExportServicesDialog.java`, `ExportSparePartsDialog.java` — по 185 строк. Между собой различаются **30 строками** (сверено `diff`): имя класса, заголовки окна, ключи `WindowStateManager`/`FileDialogPathManager`, источник списка (`DataStore.getClients/getServices/getSpareParts`) и вызов `ExportService.export*ToFile`. Остальное — одинаковый UI, обработчики, `showAlert`, сохранение состояния окна.
*Рекомендация:* один `EntityExportDialog` с параметрами (заголовок, id окна, `Supplier<List<?>>`, функция экспорта, подсказка). Экономия ~370 строк и одна точка правок.

**1.2. Три почти идентичных диалога импорта — 660 строк.**
219 / 219 / 222 строки. `ImportClientsDialog` ↔ `ImportServicesDialog`: **34 различающиеся строки**, `ImportClientsDialog` ↔ `ImportSparePartsDialog`: 37. Отличия — заголовки, подсказка формата XML, вызов `ImportService.import*FromFile`, `*Controller.refreshTable()`, текст итогового алерта.
*Рекомендация:* один `EntityImportDialog` по тому же принципу, что и в 1.1.

**1.3. Две реализации БД и общий родитель: продублированы SQL и DDL.**
- `SQLiteDatabase.createTables` (203 строки, 59–261) и `H2Database.createTables` (172 строки, 68–239) совпадают на 81% — 139 общих строк (из 172 у H2).
- `generateOrderId` — 49 строк, `SQLiteDatabase:297–345` и `H2Database:275–323`: тела **полностью совпадают** (различие только в отступах), хотя метод объявлен `abstract` в `AbstractDatabase:50`; реализацию достаточно оставить в родителе.
- `saveOrderParts` — 17 строк, идентично в `SQLiteDatabase:676–692` и `H2Database:549–565`.
- `updateAppointment` — 20 строк, идентичен во всех трёх классах: `AbstractDatabase:923–942`, `SQLiteDatabase:726–745`, `H2Database:605–624`.
- В `SQLiteDatabase` четыре переопределения — точные копии методов родителя: `addService` (358–382 против 487–511), `updateSparePart` (482–506 против 632–655), `updateAppointment` (726–745 против 923–942), `deleteAppointment` (752–761 против 949–958). Их можно удалить без изменения поведения.
- Схемы уже разошлись: в `H2Database.spare_parts` есть неиспользуемые legacy-колонки `unit_volume` и `is_liquid` (`H2Database:115–117`), в SQLite их нет.

*Рекомендация:* DDL — в один ресурс/константу с параметром диалекта (`AUTOINCREMENT`/`IDENTITY`), `generateOrderId` — в `AbstractDatabase`, лишние переопределения удалить. Это главный источник риска: любое изменение SQL нужно синхронно вносить в 2–3 места, что уже привело к расхождению.

**1.4. `AppointmentView`: два почти идентичных диалога — 337 строк при 97% совпадения.**
`showAppointmentDetails` (168 строк, 462–629) и `showAppointmentInfoOnly` (169 строк, 631–799). Различий — 6 строк: заголовок окна, ключи `WindowStateManager`, подключение CSS.
*Рекомендация:* один метод с параметром «только чтение/с действиями».

**1.5. `AutoAddSparePartService`: два метода-близнеца.**
`getSparePartsByService` (68 строк, 25–92) и `getSparePartsByServiceId` (61 строка, 99–159) совпадают на 95%: различие — только первые 7 строк, где услуга ищется по имени. Это один и тот же алгоритм с уже известным `serviceId`.
*Рекомендация:* один приватный метод `collect(serviceId)`; публичный «по имени» только резолвит услугу и делегирует.

**1.6. `CreateOrderDialog` ↔ `EditOrderDialog`: `showServiceSparePartsDialog`.**
185 и 172 строки, 89% совпадения, включая побайтово одинаковый блок из 64 строк (`CreateOrderDialog:630–693` ↔ `EditOrderDialog:677–740`). Кроме того, корректировка остатков продублирована блоками: добавление запчасти (`CreateOrderDialog:271–280` ↔ `EditOrderDialog:280–289`), восстановление остатков при отмене (`CreateOrderDialog:527–538` ↔ `EditOrderDialog` `cancelChanges`).
*Рекомендация:* общий диалог выбора запчастей и общий хелпер изменения остатков.

**1.7. Add/Edit-диалоги в `SettingsView` и `SparePartPanel`.**
- `showAddServiceSparePartDialog` (163) ↔ `showEditServiceSparePartDialog` (180) — 77% совпадения;
- `showAddToPartDialog` (170) ↔ `showEditToPartDialog` (182) — 74%;
- `SparePartPanel.showAddSparePartDialog` (189) ↔ `editSparePartDialog` (174) — 68%.
Во всех парах различие — инициализация исходными значениями.
*Рекомендация:* один метод с `initial`-параметром (`null` — режим создания).

**1.8. Отчётность считается дважды, один экземпляр используется только тестами.**
`ReportView.collectData` (49 строк) считает статусы, выручку, топ-5 услуг и топ-5 запчастей. Те же агрегаты независимо считают `ReportGenerator.generateReport` (79 строк) и `StatisticsService` (149 строк). При этом `ReportGenerator` и `StatisticsService` в прод-коде **не вызываются** (проверено поиском) — только из тестов `ReportGeneratorTest` и `StatisticsServiceTest`, т.е. ~430 строк поддерживаются параллельно с реально работающим кодом `ReportView`.
*Рекомендация:* `ReportView` берёт данные из `StatisticsService`, `ReportGenerator` рендерит PDF из общего DTO; неиспользуемые дубли удалить.

**1.9. Два класса валидации с одинаковыми правилами.**
`Validators.isValidPhone/isValidCarNumber` и `utils/ValidationUtils.isValidPhone/isValidCarNumber` — одинаковые регулярные выражения. Прод-код использует `ValidationUtils` (например, `EditClientDialog:190`), `Validators.isValid*` вызывается только из тестов; реализации при этом слегка расходятся (`trim()`). Тел-валидация (форматтеры полей) живёт в `Validators`.
*Рекомендация:* оставить один класс проверок + один класс UI-форматтеров.

**1.10. Три параллельных API доступа к данным.**
`Database` (486 строк, 55 статических методов-делегатов) → `DatabaseFactory` → `AbstractDatabase`; `DataStore` (947 строк) параллельно вызывает `DatabaseFactory.getDatabase()` 78 раз и дублирует часть фасада (`getSettingByKey`, `getAppointmentsByDate` и др.). Один и тот же метод достижим тремя способами (`Database.*`, `DataStore.*`, `DatabaseFactory.getDatabase().*`).
*Рекомендация:* один слой доступа; `Database`-фасад удалить или оставить только `init/close`.

**1.11. `FileDialogPathManager` и `WindowStateManager` — один и тот же каркас.**
Оба singleton-менеджера: `Map`-кэш + Gson + файл на ключ + `loadAll*`, `save*`, `reset*`, `createDirectoryIfNotExists`. Тела `loadAllPaths` (139–161) и `loadAllStates` (203–225), `resetLastPath` (117–134) и `resetWindowState` (166–183) структурно идентичны (отличаются только имена идентификаторов).
*Рекомендация:* общий параметризованный класс хранилища состояний.

**1.12. Точечное дублирование.**
- `OrderView.extractLocalDateTime` (56 строк) и `parseAppointmentDateTime` (62 строки) — оба парсят дату/время с дефисом и двоеточием, ~49% строк совпадает (различия — набор форматов и строгость валидации) → один парсер в `DateUtils`.
- 10 model-классов на 750 строк — геттеры/сеттеры/`toString`; часть уместно заменить `record` (Java 21), например `ColumnState`, `SortState`, `WindowState`.

## 2. Потенциальные утечки ресурсов

Проверены все 13 точек создания `FileInputStream`/`FileOutputStream`/`FileReader`/`FileWriter` — все обёрнуты в `try-with-resources`, утечек такого типа нет. Все 75 получений `Connection` также используют `try-with-resources`; проблема — в жизненном цикле пула (K1). Найденные утечки относятся к другим ресурсам: PDF-поток (K2), каталоговые потоки `Files.walk` (K3), statement'ы (R2.1), бесконечная анимация и запланированная задача (R2.2–R2.3).

### Критичные

**K1. Восстановление БД из бэкапа при живом пуле соединений: потеря данных и/или неработающее восстановление.**
`BackupService.restoreBackup` (123–187): создать копию → `Files.delete(currentDb)` (139–143) → распаковать архив (146–174) → `DataStore.load()` (179). Пул Hikari при этом не закрывается и не пересоздаётся — `SettingsController.performRestoreBackup` (339–341) просто вызывает сервис.

Следствия (ожидаемое поведение драйвера SQLite/Hikari; для точного подтверждения нужен запуск на целевой ОС):
- Windows: файл `autoservice.db` открыт соединениями пула → `Files.delete` завершается ошибкой, восстановление не работает;
- Linux/macOS: удаление проходит, но пул продолжает смотреть на старый inode → `DataStore.load()` читает старые данные, новые записи уходят в удалённый файл (потеря после перезапуска);
- текущая БД удаляется **до** проверки архива: повреждённый/чужой zip → данные уничтожены, а созданная перед этим копия не используется для отката.

*Рекомендация:* валидировать архив до удаления, закрыть пул (`Database.close()`), распаковывать во временный файл и атомарно заменять, затем `Database.init()` + `DataStore.load()`; при ошибке — откат из созданной копии.

**K2. `PrintOrderDialog`: PDF-поток не закрывается при исключении, шрифты — только Windows.**
`generatePDF` (50–97): `PdfWriter writer = new PdfWriter(filePath)` (52), `PdfDocument pdfDoc` (53), `Document doc` (56) создаются без `try-with-resources`, а `doc.close()` вызывается только в конце успешного сценария (88). Любое исключение до этой строки оставляет открытыми файл и writer.
Исключение вне Windows практически гарантировано: шрифты грузятся по жёстко заданному пути `C:/Windows/Fonts/arial.ttf` и `arialbd.ttf` (60–65), при этом README заявляет поддержку Linux и macOS. Итог: утечка файлового дескриптора, пустой/битый PDF на диске (на Windows он ещё и блокирует повторную запись).
*Рекомендация:* `try (PdfWriter w = …; PdfDocument d = …; Document doc = …)`, шрифт — из ресурсов проекта (с fallback), платформенные пути не хардкодить. В `ReportGenerator.exportToPdf` (174–176) сделано правильно — стоит повторить этот шаблон.

**K3. `BackupService`: четыре незакрытых потока `Files.walk`.**
Строки 70 и 89 (создание бэкапа), 200 (`cleanupOldBackups`), 232 (`getAvailableBackups`) — `Files.walk(...).forEach(...)` без `try-with-resources`; каждый вызов держит открытый `DirectoryStream`/дескриптор до сборки мусора. Бэкап запускается ежедневно по расписанию и из UI; на Windows открытые handles на каталог `backups/` могут препятствовать удалению и переименованию.
*Рекомендация:* `try (Stream<Path> s = Files.walk(dir)) { … }` во всех четырёх местах.

### Рекомендации

**R2.1. `AbstractDatabase`: `Statement`/`PreparedStatement` не закрываются в трёх методах.**
`getServiceSparePartsByServiceId` (1017–1058), `getServiceSparePartsListsByServiceId` (1322–1361), `getToPartsByCarModel` (1544–1586): внутри `try (Connection conn = getConnection())` создаются `Statement stmt = conn.createStatement()` (1032/1337/1559) и `PreparedStatement pstmt` (1038/1343/1565); `ResultSet` закрывается явно (`rs.close()`), statement — нет. Hikari при возврате соединения в пул закрывает прослеженные statement'ы, поэтому «вечной» утечки нет, но код молча полагается на поведение пула. Дополнительно: при конкретном `serviceId` созданный `Statement stmt` не используется (всегда уходит в `prepareStatement`) — лишний объект и мёртвый код.
*Рекомендация:* `try (PreparedStatement ps = conn.prepareStatement(sql))` и единый SQL без ветвления (`WHERE (? = -1 OR service_id = ?)` или два метода).

**R2.2. `ScheduleService`: задача бэкапа не отменяется.**
`cancelScheduledBackup` (144–149) только обнуляет ссылку `backupTask`, не отменяя `ScheduledFuture`; `saveSettings(false, …)` (72–87) вообще не отменяет задачу. В результате после выключения авто-бэкапа в настройках ежедневная задача продолжает выполняться, а повторное включение не перепланирует её (`backupTask != null`). Планировщик закрывается только в обработчике закрытия приложения (`App:105–107`).
*Рекомендация:* хранить `ScheduledFuture<?>`, отменять его в `cancelScheduledBackup()` и при выключении настройки, затем планировать заново.

**R2.3. `LoadingIndicator`: бесконечная анимация не останавливается (и класс не используется).**
`Timeline` с `INDEFINITE` (112–117) запускается при показе оверлея и никогда не останавливается; после удаления оверлея анимация продолжает тикать. Сейчас класс не вызывается из прод-кода (есть неиспользуемый импорт в `SettingsController:14`), поэтому утечка отложенная, но при подключении будет на каждую длительную операцию.
*Рекомендация:* `timeline.stop()` при снятии оверлея либо удалить класс.

## 3. Потенциальные `NullPointerException`

Точечные lookup-методы (`*ById`, `getSettingByKey`) проверяются на `null`: ни в одном из 32 обращений к `*ById`-методам не найдено разыменования без проверки в том же блоке; выбор в таблицах защищён (41 место, 3 исключения — диалоги экспорта, где выбор предустановлен `selectFirst()`); во всех кастомных ячейках `updateItem` есть проверки. Риски ниже — там, где проверки нет, а `null` допускается контрактом.

### Критичные

**K4. `WorkOrder.getClient()` / `Appointment.getClient()`: разыменование без проверки.**
`WorkOrder()` явно присваивает `client = null` (`WorkOrder.java:33`), `Appointment` — тоже (`Appointment.java:20–30`, `45–47`). При этом без проверки:
- `OrderView` — фабрики значений колонок «Клиент» и «Автомобиль»: `order.getClient().getLastName()` (298–299), `cellData.getValue().getClient().getCarModel()` (308–309). NPE здесь ломает отрисовку всей таблицы заказов;
- `OrderDetailsDialog:38, 42`; `EditOrderDialog:92`; `PrintOrderDialog:112–134, 139–149`;
- `DataStore.deleteClient:225` — `order.getClient().getId()` при удалении клиента;
- `WorkOrder.toString():203` и `Appointment.toString():121` — оба разыменовывают `client`; `WorkOrder.toString()` вызывается неявно из `ComboBox<WorkOrder>` (`AppointmentView:830`);
- `AppointmentView:1016–1019` — внутри проверки `existingAppointment != null` клиент разыменован без проверки (`client.getLastName()`).

При этом рядом проверка есть: `AppointmentView:843`, `859` используют `item.getClient() != null`. То есть контракт модели трактуется в разных местах по-разному.
*Условие срабатывания:* заказ/запись без связанного клиента. Загрузка из БД (`AbstractDatabase.getAllOrders`, `LEFT JOIN`) подставляет «пустого» `Client`, поэтому сейчас через штатный сценарий `null` не приходит; но `new WorkOrder()` (`CreateOrderDialog:442`) и любые внешние вызовы модели это допускают.
*Рекомендация:* либо `Objects.requireNonNull(client)` в конструкторах/сеттерах `WorkOrder`/`Appointment` (и проверить все места создания), либо проверки `getClient() != null` во всех перечисленных местах с отображением «Без клиента».

### Рекомендации

**R3.1. Распаковка `Spinner.getValue()` без проверки (`SettingsView`).**
`double quantity = quantitySpinner.getValue();` — строки 1272 и 1389; `int backupRetentionValue = retentionSpinner.getValue();` — 2453. Спиннеры количества редактируемые (`setEditable(true)`, 1242 и 1359): `getValue()` может вернуть `null` (пустое/некорректное значение в редакторе), и распаковка даст NPE. Дополнительно в 1272/1389 значение читается до проверок `service == null` / `sparePart == null`.
*Рекомендация:* читать в `Double q = spinner.getValue();` с проверкой на `null` и сообщением валидации.

**R3.2. `TableStateManager.restoreColumnStates`: распаковка `Integer index`.**
Строка 197: `sortedColumns.sort(Comparator.comparingInt(ColumnState::getIndex))`, где `ColumnState.getIndex()` возвращает `Integer` (`ColumnState.java:17`). В JSON из `config/table-state/*.json` поле может отсутствовать или быть `null` (старый файл, ручная правка) → NPE. Он перехватывается общим `catch (Exception)` (272–274) и превращается в «состояние таблицы не восстановлено» без явной диагностики.
*Рекомендация:* `Comparator.comparingInt(cs -> cs.getIndex() == null ? Integer.MAX_VALUE : cs.getIndex())` и валидация при десериализации.

**R3.3. `OilHelper`: разыменование `getUnitType()` без проверки (мёртвый код).**
`oil.getUnitType().equals("л")` (95, 119) и `.contains("л")` (123). Колонка `unit_type` в схеме допускает `NULL` (`DEFAULT 'шт'` без `NOT NULL`), а `getAllSpareParts` (605–619) значение не нормализует. Класс не вызывается нигде (мёртвый код — ни в прод-коде, ни в тестах), поэтому риск отложенный.
*Рекомендация:* `"л".equals(oil.getUnitType())` и нормализация при чтении из БД.

**R3.4. `AbstractDatabase.getConnection()` при неинициализированном пуле.**
`return dataSource.getConnection()` (140–142) без проверки `dataSource != null`; при этом `DatabaseFactory.getDatabase()` (15–20) лениво создаёт `new SQLiteDatabase()` **без** вызова `init()`. Любой доступ к данным до `Database.init()` (новый вход, скрипт, тест) даст NPE вместо понятной ошибки «БД не инициализирована».
*Рекомендация:* явная проверка с `IllegalStateException`/логированием; инициализацию — в фабрику.

## 4. Слишком длинные методы (более 50 строк)

Статистика: в прод-коде 1 149 методов, из них **84 длиннее 50 строк** (7,3%), 42 длиннее 100 и 30 длиннее 150. В этих 84 методах — 10 273 строки, то есть ~42% прод-кода. Сам по себе размер метода дефектом не является, но именно здесь сосредоточено дублирование (п. 1.4–1.7 — это всё длинные UI-методы) и основная сложность сопровождения.

Распределение по каталогам: `views` — 44, `dialogs` — 16, `services` — 12, корневые классы (БД, модель, `App`) — 9, `controllers` — 2, `utils` — 1. Больше всего в `SettingsView` — 17 методов (максимум 213 строк).

### Критичные

Отдельно не выделяются: длинные методы не ломают поведение. Но четыре из них содержат бизнес-логику и должны быть разбиты первыми:

- `SQLiteDatabase.migrateTables` — 227 строк (765–991): миграции, DDL и PRAGMA в одном теле;
- `SQLiteDatabase.createTables` — 203 строки (59–261) и `H2Database.createTables` — 172 строки (68–239);
- `EditOrderDialog.saveOrder` — 178 строк (402–579): валидация, списание остатков, запись заказа, запись в календарь;
- `AbstractDatabase.getAllOrders` — 89 строк (681–769): три запроса и сборка агрегата.

### Рекомендации

Направления разбиения (по убыванию эффекта):

1. **UI-диалоги (60 из 84 методов)** — вынести сборку секций в приватные методы (`createHeader`, `createClientSection`, `createServicesTable`), обработчики — в отдельные методы; одинаковые пары add/edit объединить (см. п. 1.7); `CreateOrderDialog.showAsync` (489 строк) и `EditOrderDialog.show` (333) — разобрать на «создать окно», «собрать форму», «повесить обработчики».
2. **`createTables`/`migrateTables`** — по методу на таблицу и на миграцию, список миграций (`List<Migration>`), DDL — в константы или ресурсы; заодно устранит дублирование из п. 1.3.
3. **`saveOrder`** — разделить на `validateInput`, `applyStockChanges`, `persistOrder`, `createOrUpdateAppointment`.
4. **PDF-рендеринг** — `ReportGenerator.exportToPdf` (100 строк) и `PrintOrderDialog` (274 строки) должны использовать общий хелпер построения таблиц/шрифтов.

### Топ-20 самых длинных методов

| Строк | Метод | Файл:строки |
|---:|---|---|
| 489 | `showAsync` | `dialogs/CreateOrderDialog.java:53–541` |
| 333 | `show` | `dialogs/EditOrderDialog.java:68–400` |
| 261 | `showAsync` | `dialogs/EditClientDialog.java:42–302` |
| 227 | `migrateTables` | `SQLiteDatabase.java:765–991` |
| 213 | `createBackupPanel` | `views/SettingsView.java:2271–2483` |
| 203 | `createTables` | `SQLiteDatabase.java:59–261` |
| 194 | `showAddEditDialog` | `dialogs/AddServicePartDialog.java:62–255` |
| 189 | `showAddSparePartDialog` | `views/SparePartPanel.java:245–433` |
| 187 | `showAddAppointmentWithOrderDialog` | `views/AppointmentView.java:801–987` |
| 185 | `showServiceSparePartsDialog` | `dialogs/CreateOrderDialog.java:561–745` |
| 182 | `showEditToPartDialog` | `views/SettingsView.java:2024–2205` |
| 180 | `showEditServiceSparePartDialog` | `views/SettingsView.java:1662–1841` |
| 178 | `saveOrder` | `dialogs/EditOrderDialog.java:402–579` |
| 174 | `editSparePartDialog` | `views/SparePartPanel.java:435–608` |
| 172 | `createTables` | `H2Database.java:68–239` |
| 172 | `showServiceSparePartsDialog` | `dialogs/EditOrderDialog.java:622–793` |
| 170 | `showAddToPartDialog` | `views/SettingsView.java:1850–2019` |
| 169 | `show` | `dialogs/ImportSparePartsDialog.java:38–206` |
| 169 | `showAppointmentInfoOnly` | `views/AppointmentView.java:631–799` |
| 168 | `showAppointmentDetails` | `views/AppointmentView.java:462–629` |

### Полный список методов длиннее 50 строк (84, по файлам)

<details>
<summary>Развернуть таблицу</summary>

| Файл:строки | Метод | Строк |
|---|---|---:|
| `AbstractDatabase.java:681–769` | `getAllOrders` | 89 |
| `AbstractDatabase.java:187–239` | `addClient` | 53 |
| `App.java:28–122` | `start` | 95 |
| `DataStore.java:71–162` | `save` | 92 |
| `H2Database.java:68–239` | `createTables` | 172 |
| `H2Database.java:423–477` | `addOrder` | 55 |
| `SQLiteDatabase.java:765–991` | `migrateTables` | 227 |
| `SQLiteDatabase.java:59–261` | `createTables` | 203 |
| `SQLiteDatabase.java:515–592` | `addOrder` | 78 |
| `SQLiteDatabase.java:391–456` | `addSparePart` | 66 |
| `controllers/SettingsController.java:98–153` | `loadServiceSparePartsRows` | 56 |
| `controllers/SettingsController.java:199–254` | `loadToRemovePartsRows` | 56 |
| `dialogs/AddServicePartDialog.java:62–255` | `showAddEditDialog` | 194 |
| `dialogs/CreateOrderDialog.java:53–541` | `showAsync` | 489 |
| `dialogs/CreateOrderDialog.java:561–745` | `showServiceSparePartsDialog` | 185 |
| `dialogs/EditClientDialog.java:42–302` | `showAsync` | 261 |
| `dialogs/EditClientDialog.java:307–361` | `addCarRow` | 55 |
| `dialogs/EditOrderDialog.java:68–400` | `show` | 333 |
| `dialogs/EditOrderDialog.java:402–579` | `saveOrder` | 178 |
| `dialogs/EditOrderDialog.java:622–793` | `showServiceSparePartsDialog` | 172 |
| `dialogs/EditServiceDialog.java:42–194` | `showEditDialog` | 153 |
| `dialogs/ExportClientsDialog.java:28–170` | `show` | 143 |
| `dialogs/ExportServicesDialog.java:28–170` | `show` | 143 |
| `dialogs/ExportSparePartsDialog.java:28–170` | `show` | 143 |
| `dialogs/ImportClientsDialog.java:37–203` | `show` | 167 |
| `dialogs/ImportServicesDialog.java:37–203` | `show` | 167 |
| `dialogs/ImportSparePartsDialog.java:38–206` | `show` | 169 |
| `dialogs/OrderDetailsDialog.java:25–86` | `show` | 62 |
| `services/AutoAddSparePartService.java:25–92` | `getSparePartsByService` | 68 |
| `services/AutoAddSparePartService.java:99–159` | `getSparePartsByServiceId` | 61 |
| `services/BackupService.java:37–116` | `createBackup` | 80 |
| `services/BackupService.java:123–187` | `restoreBackup` | 65 |
| `services/ImportService.java:285–345` | `importClientsFromXml` | 61 |
| `services/ImportService.java:151–208` | `importFromXml` | 58 |
| `services/ImportService.java:399–451` | `importServicesFromXml` | 53 |
| `services/ReportGenerator.java:173–272` | `exportToPdf` | 100 |
| `services/ReportGenerator.java:87–165` | `generateReport` | 79 |
| `services/TableStateManager.java:176–275` | `restoreColumnStates` | 100 |
| `services/TableStateManager.java:34–107` | `saveTableState` | 74 |
| `utils/OilHelper.java:104–155` | `addOilAndPartsToOrder` | 52 |
| `views/AppointmentView.java:801–987` | `showAddAppointmentWithOrderDialog` | 187 |
| `views/AppointmentView.java:631–799` | `showAppointmentInfoOnly` | 169 |
| `views/AppointmentView.java:462–629` | `showAppointmentDetails` | 168 |
| `views/AppointmentView.java:182–338` | `loadWeekView` | 157 |
| `views/AppointmentView.java:990–1140` | `showEditAppointmentDialog` | 151 |
| `views/AppointmentView.java:71–168` | `create` | 98 |
| `views/AppointmentView.java:340–430` | `loadMonthView` | 91 |
| `views/ClientView.java:160–267` | `createClientTable` | 108 |
| `views/ClientView.java:50–127` | `create` | 78 |
| `views/DashboardView.java:348–464` | `createAppointmentsWeekBox` | 117 |
| `views/DashboardView.java:77–136` | `doRefresh` | 60 |
| `views/DashboardView.java:169–219` | `createActionButton` | 51 |
| `views/OrderView.java:257–402` | `setupTableColumns` | 146 |
| `views/OrderView.java:62–164` | `create` | 103 |
| `views/OrderView.java:173–246` | `createAdvancedFilterPanel` | 74 |
| `views/OrderView.java:623–684` | `parseAppointmentDateTime` | 62 |
| `views/OrderView.java:475–533` | `applyFilters` | 59 |
| `views/OrderView.java:561–616` | `extractLocalDateTime` | 56 |
| `views/ReportView.java:52–189` | `show` | 138 |
| `views/ServicePanel.java:39–145` | `createServicesPanel` | 107 |
| `views/ServicePartsTreeTableView.java:70–138` | `createTreeTable` | 69 |
| `views/ServicePartsView.java:118–207` | `createTable` | 90 |
| `views/SettingsView.java:2271–2483` | `createBackupPanel` | 213 |
| `views/SettingsView.java:2024–2205` | `showEditToPartDialog` | 182 |
| `views/SettingsView.java:1662–1841` | `showEditServiceSparePartDialog` | 180 |
| `views/SettingsView.java:1850–2019` | `showAddToPartDialog` | 170 |
| `views/SettingsView.java:1421–1583` | `showAddServiceSparePartDialog` | 163 |
| `views/SettingsView.java:776–937` | `createToPartsPanel` | 162 |
| `views/SettingsView.java:1021–1180` | `createServicePartsPanel` | 160 |
| `views/SettingsView.java:537–689` | `createServiceSparePartsPanel` | 153 |
| `views/SettingsView.java:1299–1417` | `showEditServicePartDialog` | 119 |
| `views/SettingsView.java:1192–1297` | `showAddServicePartDialog` | 106 |
| `views/SettingsView.java:691–772` | `refreshServiceSparePartsRows` | 82 |
| `views/SettingsView.java:2518–2596` | `createImportExportPanel` | 79 |
| `views/SettingsView.java:939–1012` | `refreshToParts` | 74 |
| `views/SettingsView.java:260–327` | `createSettingsTable` | 68 |
| `views/SettingsView.java:467–533` | `createAutoPartsPanel` | 67 |
| `views/SettingsView.java:2213–2267` | `migrateOldDataToNewStructure` | 55 |
| `views/SettingsView.java:178–228` | `createSettingsAppPanel` | 51 |
| `views/SparePartPanel.java:245–433` | `showAddSparePartDialog` | 189 |
| `views/SparePartPanel.java:435–608` | `editSparePartDialog` | 174 |
| `views/SparePartPanel.java:51–207` | `createSparePartsPanel` | 157 |
| `views/StockPanel.java:49–171` | `createStockPanel` | 123 |
| `views/StockPanel.java:205–304` | `editStockDialog` | 100 |

</details>

## Приоритетный план

1. **K1** — восстановление БД (потеря данных, неработающий restore) — наивысший приоритет.
2. **K2/K3** — утечки в экспорте PDF и в бэкапах (дескрипторы, битые файлы, Windows-only шрифт).
3. **K4** — NPE-кластер `client`: согласовать контракт модели и убрать разыменования без проверок.
4. **R2.2/R2.1** — отмена запланированной задачи бэкапа и закрытие statement'ов.
5. **R3.1–R3.4** — точечные проверки на `null`.
6. **Дублирование 1.1–1.3** — диалоги импорта/экспорта и SQL/схема БД: наибольший эффект на стоимость поддержки.
7. **Длинные методы** — постепенно, начиная с `migrateTables` и `saveOrder`, при правках затронутых файлов.

## Прочие наблюдения (вне четырёх запрошенных категорий)

- `SettingService.setSettingValue` пишет значение настройки в лог в открытом виде: `logger.info("  after setValue: ... newValue={}", setting.getValue())` (69), хотя остальные сообщения того же метода маскируют значение как `[REDACTED]`. Если в настройках появятся секреты — утечка в лог.
- `ExceptionHandler.UI_HANDLER` (21) нигде не устанавливается (`setDefaultUncaughtExceptionHandler` отсутствует) — глобальный перехватчик мёртв; при этом NPE маппится в сообщение «Клиент не найден» (43–44), что скрывает реальные причины ошибок.
- `System.out.println` — 10 вызовов в `SettingsView` (отладочный вывод в прод-коде).
- Мёртвый/тестовый код: `OilHelper` (155 строк) и `LoadingIndicator` (157) не вызываются вовсе; `StatisticsService` (149) и `ReportGenerator` (277) вызываются только тестами; фасад `Database` (486 строк) — избыточный слой (см. п. 1.10).
- H2-схема содержит неиспользуемые legacy-колонки `unit_volume`, `is_liquid` (`H2Database:115–117`) и расходится с SQLite (см. п. 1.3).

## Ограничения ревью

- Сборка, тесты, SpotBugs и Checkstyle не запускались (в среде нет JDK) — выводы основаны на статическом анализе; часть NPE-рисков стоит подтвердить запуском.
- Проценты совпадения получены нормализованным сравнением (игнорируются комментарии, строковые литералы и имена), поэтому могут незначительно отличаться от ручной сверки; для крупных случаев приведён точный `diff`.
- Производительность, безопасность (кроме отмеченного) и UX не были целью ревью.
