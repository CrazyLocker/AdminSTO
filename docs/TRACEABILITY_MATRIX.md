# Матрица трассируемости требований и тестов

## Система автосервиса AdminSTO 1.2.0

> **Назначение:** связь функциональных требований с тестами текущей версии.
> **Актуализировано:** 18.11.2026
> **Версия ПО:** 1.2.0

Обозначения: ✅ — требование реализовано и покрыто автотестами; 🟡 —
реализовано, проверяется частично (UI/ручной сценарий). Ссылки — на классы и
методы `src/test/java`.

## 1. Клиенты и автомобили (8)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-CL-001 | Добавление клиента | ✅ | `DataStoreTest.testAddClient`, `ClientViewUITest.addedClientAppearsInTable` |
| REQ-CL-002 | Хранение нескольких автомобилей | ✅ | `DataStoreRelationsTest` (автомобили клиента) |
| REQ-CL-003 | Модель, номер и пробег автомобиля | ✅ | `ClientTest.testGettersAndSetters`, `DataStoreRelationsTest` |
| REQ-CL-004 | Редактирование/удаление клиента | ✅ | `DataStoreTest.testUpdateClient`, `DatabaseTest.testDeleteClient`, `DataStoreRelationsTest.deleteClientRemovesRelatedOrders` |
| REQ-CL-005 | Валидация телефона и госномера | ✅ | `ValidationUtilsTest` |
| REQ-CL-006 | Формат телефона в UI | ✅ | `FormattersTest`, `ClientViewUITest` |
| REQ-CL-007 | Поиск по ФИО/телефону | ✅ | `ClientViewUITest.searchByNameFiltersTable`, `searchByPhoneFiltersTable` |
| REQ-CL-008 | Звёзды ТО и статус тюнинга в карточке | ✅ | `LoyaltyServiceTest`, `ClientViewUITest.loyaltyColumnsShowClientStatus` |

## 2. Услуги (6)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-SV-001 | Создание услуги | ✅ | `DatabaseTest`, `DataStoreTest.testAddService` |
| REQ-SV-002 | Название и цена | ✅ | `ServiceTest` |
| REQ-SV-003 | Категория `REGULAR`/`MAINTENANCE`/`TUNING` | ✅ | `ServiceTest`, `LoyaltyServiceTest.categoriesArePersistedForCatalogEntities` |
| REQ-SV-004 | Длительность | ✅ | `ServiceTest` |
| REQ-SV-005 | Обновление услуги | ✅ | `DataStoreTest.testUpdateService` |
| REQ-SV-006 | Удаление услуги | ✅ | `DatabaseTest`, `DataStoreTest` |

## 3. Запчасти и склад (7)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-SP-001 | Создание запчасти | ✅ | `DatabaseTest.testAddSparePart`, `DataStoreTest.testAddSparePart` |
| REQ-SP-002 | Цены закупки и розничная | ✅ | `SparePartTest` |
| REQ-SP-003 | Остаток и минимальный запас | ✅ | `SparePartTest`, `StockHelperTest` |
| REQ-SP-004 | Единица измерения | ✅ | `SparePartTest` |
| REQ-SP-005 | Связи «услуга → запчасть» | ✅ | `DataStoreRelationsTest`, `AutoAddSparePartServiceTest` |
| REQ-SP-006 | Автодобавление запчастей в заказ | ✅ | `AutoAddSparePartServiceTest`, `CreateOrderDialogTest` |
| REQ-SP-007 | Приход и списание остатков | ✅ | `IntegrationStockTest`, `StockHelperTest` |

## 4. Заказы (10)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-WO-001 | Создание заказа | ✅ | `DataStoreTest.testAddOrderWithServices`, `testAddOrderWithSpareParts` |
| REQ-WO-002 | Несколько услуг/запчастей | ✅ | `WorkOrderTest`, `DataStoreTest.testAddOrderWithServicesAndParts` |
| REQ-WO-003 | Расчёт суммы | ✅ | `WorkOrderTest`, `WorkOrderLoyaltyDiscountTest` |
| REQ-WO-004 | Статусы заказа | ✅ | `WorkOrderTest.testWorkOrderStatuses`, `DataStoreTest.testOrderStatus` |
| REQ-WO-005 | Списание и возврат остатков | ✅ | `IntegrationStockTest`, `CreateOrderDialogTest` |
| REQ-WO-006 | Редактирование/удаление | ✅ | `DataStoreTest.testUpdateOrder`, `testDeleteOrder` |
| REQ-WO-007 | PDF заказа-наряда | ✅ | `PrintOrderDialogTest` |
| REQ-WO-008 | Фильтры и поиск | ✅ | `OrderViewUITest` (19 тестов) |
| REQ-WO-009 | Закрытый заказ не редактируется | ✅ | `WorkOrderTest`, `OrderViewUITest` |
| REQ-WO-010 | Скидки в строках заказа | ✅ | `WorkOrderLoyaltyDiscountTest`, `LoyaltyServiceTest` |

## 5. Статусы заказов (3)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-ST-001 | «Новый» | ✅ | `WorkOrderTest.testWorkOrderStatuses` |
| REQ-ST-002 | «В работе» | ✅ | `WorkOrderTest.testWorkOrderStatuses`, `DataStoreTest.testGetActiveOrdersCount` |
| REQ-ST-003 | «Закрыт» | ✅ | `WorkOrderTest.testWorkOrderStatuses`, `LoyaltyServiceTest` |

## 6. Записи на обслуживание (10)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-AP-001 | Создание записи | ✅ | `DataStoreTest.testAddAppointment`, `DatabaseTest.testAddAppointment` |
| REQ-AP-002 | Привязка к клиенту и заказу | ✅ | `AppointmentTest`, `IntegrationTest` |
| REQ-AP-003 | Мастер и услуга | ✅ | `AppointmentTest.testAppointmentMasterNames`, `testGettersAndSetters` |
| REQ-AP-004 | Дата и время | ✅ | `AppointmentTest`, `DateUtilsTest` |
| REQ-AP-005 | Статусы записи | ✅ | `AppointmentTest.testStatusConstants` |
| REQ-AP-006 | Обновление записи | ✅ | `DataStoreTest.testUpdateAppointment` |
| REQ-AP-007 | Фильтр по дате | ✅ | `DataStoreTest.testGetAppointmentsByDate` |
| REQ-AP-008 | Переключатель «Неделя/Месяц» | 🟡 | `AppointmentCalendarUITest.weekViewIsStyledWithThemeClasses`, `monthViewShowsStatusDotsAndTodayHighlight` |
| REQ-AP-009 | Легенда и счётчик записей | 🟡 | `AppointmentCalendarUITest.dayHeaderShowsAppointmentCounter` |
| REQ-AP-010 | Запись видна на дашборде | 🟡 | `AppointmentVisibilityUITest.appointmentIsVisibleOnDashboardAndAppointmentTab` |

## 7. Статусы записей (4)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-AS-001 | «Новая» | ✅ | `AppointmentTest.testStatusConstants` |
| REQ-AS-002 | «Подтверждена» | ✅ | `AppointmentTest.testStatusConstants` |
| REQ-AS-003 | «Выполнено» | ✅ | `AppointmentTest.testStatusConstants` |
| REQ-AS-004 | «Отменена» | ✅ | `AppointmentTest.testStatusConstants` |

## 8. Валидация данных (8)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-VD-001 | Телефон | ✅ | `ValidationUtilsTest` |
| REQ-VD-002 | Очистка телефона | ✅ | `ValidationUtilsTest`, `FormattersTest` |
| REQ-VD-003 | Госномер | ✅ | `ValidationUtilsTest` |
| REQ-VD-004 | Нормализация номера | ✅ | `ValidationUtilsTest` |
| REQ-VD-005 | Допустимые буквы | ✅ | `ValidationUtilsTest` |
| REQ-VD-006 | Дата | ✅ | `DateUtilsTest` |
| REQ-VD-007 | Форматирование даты | ✅ | `DateUtilsTest` |
| REQ-VD-008 | Парсинг даты | ✅ | `DateUtilsTest` |

## 9. База данных (12)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-DB-001 | Подключение SQLite | ✅ | `DatabaseTest.testCreateTables`, `DatabaseSchemaTest` |
| REQ-DB-002 | Создание 14 таблиц и индексов | ✅ | `DatabaseTest.testCreateTables`, `DatabaseSchemaTest` |
| REQ-DB-003 | Сохранение клиента | ✅ | `DatabaseTest.testAddClient` |
| REQ-DB-004 | Загрузка клиентов | ✅ | `DatabaseTest.testGetClientById`, `DataStoreTest.testInitialLoad` |
| REQ-DB-005 | Обновление клиента | ✅ | `DatabaseTest.testUpdateClient` |
| REQ-DB-006 | Заказ: сохранение | ✅ | `DatabaseTest.testAddOrderWithServices` |
| REQ-DB-007 | Заказ: обновление | ✅ | `DatabaseTest.testUpdateOrderStatus` |
| REQ-DB-008 | Заказ: удаление | ✅ | `DatabaseTest.testDeleteOrder` |
| REQ-DB-009 | Услуга | ✅ | `DatabaseTest.testAddService` |
| REQ-DB-010 | Запчасть | ✅ | `DatabaseTest.testAddSparePart` |
| REQ-DB-011 | Запись | ✅ | `DatabaseTest.testAddAppointment` |
| REQ-DB-012 | Миграции существующей БД | ✅ | `SQLiteMigrationsTest`, `DatabaseSchemaTest` |

## 10. DataStore (10)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-DS-001 | Загрузка при старте | ✅ | `DataStoreTest.testInitialLoad`, `testLoadData` |
| REQ-DS-002 | CRUD клиента | ✅ | `DataStoreTest.testAddClient`, `testUpdateClient` |
| REQ-DS-003 | CRUD услуги | ✅ | `DataStoreTest.testAddService`, `testUpdateService` |
| REQ-DS-004 | CRUD запчасти | ✅ | `DataStoreTest.testAddSparePart`, `testDeleteSparePart` |
| REQ-DS-005 | CRUD заказа | ✅ | `DataStoreTest.testAddOrderWithServices`, `testUpdateOrder`, `testDeleteOrder` |
| REQ-DS-006 | CRUD записи | ✅ | `DataStoreTest.testAddAppointment`, `testUpdateAppointment` |
| REQ-DS-007 | Удаление из кэша по `id` | ✅ | `DataStorePersistenceTest`, `DataStoreRelationsTest` |
| REQ-DS-008 | Связи услуг и запчастей | ✅ | `DataStoreRelationsTest` |
| REQ-DS-009 | Настройки | ✅ | `SettingServiceTest`, `DataStoreRelationsTest` |
| REQ-DS-010 | Изоляция состояния между тестами | ✅ | `DataStoreTest.testDataIsolation`, `BaseTest` |

## 11. Расходники ТО (3)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-TO-001 | Набор запчастей по модели авто | ✅ | `DataStoreRelationsTest`, `DatabaseTest` |
| REQ-TO-002 | 13 моделей GWM | ✅ | `AppConstantsTest` |
| REQ-TO-003 | Ручной ввод модели | 🟡 | `AppConstantsTest`, ручной сценарий в настройках |

## 12. Резервное копирование (6)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-BU-001 | ZIP с БД и состояниями | ✅ | `BackupServiceTest` |
| REQ-BU-002 | Ручной и автоматический бэкап | ✅ | `BackupServiceTest`, `ScheduleServiceTest` |
| REQ-BU-003 | Ограничение количества копий | ✅ | `BackupServiceTest`, `BackupServiceLifecycleTest` |
| REQ-BU-004 | Проверка архива и атомарная замена | ✅ | `BackupServiceLifecycleTest` |
| REQ-BU-005 | Откат при ошибке | ✅ | `BackupServiceLifecycleTest` |
| REQ-BU-006 | Настройки бэкапа | ✅ | `SettingServiceTest`, `ScheduleServiceTest` |

## 13. Состояние таблиц (6)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-TS-001 | Ширина колонок | ✅ | `TableStateManagerTest` |
| REQ-TS-002 | Порядок колонок | ✅ | `TableStateManagerTest` |
| REQ-TS-003 | Видимость колонок | ✅ | `TableStateManagerTest` |
| REQ-TS-004 | Сортировка | ✅ | `TableStateManagerTest` |
| REQ-TS-005 | Восстановление при запуске | ✅ | `TableStateManagerTest`, `JsonStateManagerTest` |
| REQ-TS-006 | Файлы `config/table-state/*.json` | ✅ | `TableStateManagerTest` |

## 14. Темы оформления (5)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-UI-001 | 7 тем `ThemeManager.AppTheme` | ✅ | `ThemeManagerTest.themeIdentifiersAreMapped` |
| REQ-UI-002 | Сохранение в `config/theme.txt` | ✅ | `ThemePreferencesTest` |
| REQ-UI-003 | Применение темы при старте | ✅ | `ThemeSettingsUITest.startupAppliesThemeFromFileAndShowsRestartHint` |
| REQ-UI-004 | Светлая/тёмная тема дашборда | ✅ | `DashboardThemeUITest` |
| REQ-UI-005 | Тема в диалоге склада | ✅ | `StockOperationDialogThemeUITest` |

## 15. Календарь: визуальные требования (4)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-AP-011 | Недельная сетка со стилями | ✅ | `AppointmentCalendarUITest.weekViewIsStyledWithThemeClasses` |
| REQ-AP-012 | Полоса статуса заказа | ✅ | `AppointmentCalendarUITest.appointmentStripeReflectsOrderStatus` |
| REQ-AP-013 | Счётчик записей в заголовке дня | ✅ | `AppointmentCalendarUITest.dayHeaderShowsAppointmentCounter` |
| REQ-AP-014 | Месячная сетка и «сегодня» | ✅ | `AppointmentCalendarUITest.monthViewShowsStatusDotsAndTodayHighlight` |

## 16. Таблицы: «зебра» и подсветка (4)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-UI-006 | Чередование строк | ✅ | `TableStripingUITest.rowsAlternateTheirBackground` |
| REQ-UI-007 | «Зебра» в тёмной теме | ✅ | `TableStripingUITest.rowsKeepAlternatingInDarkTheme` |
| REQ-UI-008 | Голубая подсветка выбора | ✅ | `TableStripingUITest.selectedRowIsHighlightedWithAccentColor` |
| REQ-UI-009 | Стили в диалогах | ✅ | `TableStripingUITest`, `StockOperationDialogThemeUITest` |

## 17. Лояльность (6)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-LY-001 | Звезда ТО за интервал 5 000–10 000 км | ✅ | `LoyaltyServiceTest.intervalBoundariesAreInclusive`, `twoMaintenanceVisitsGiveTwoStarsWithoutDiscount` |
| REQ-LY-002 | Максимум 5 звёзд | ✅ | `LoyaltyServiceTest.fiveMaintenanceVisitsGiveMaxStarsAndDiscount` |
| REQ-LY-003 | Скидки 3/4/5 звёзд | ✅ | `LoyaltyServiceTest.maintenanceStatusShowsStarsAndDiscount` |
| REQ-LY-004 | Stage 1/2/3 тюнинга | ✅ | `LoyaltyServiceTest.tuningStageBoundariesMatchDocumentation`, `tuningStageTransitionFromStage1ToStage2` |
| REQ-LY-005 | Скидки по категориям строк | ✅ | `LoyaltyServiceTest.discountIsAppliedByLineCategory`, `WorkOrderLoyaltyDiscountTest` |
| REQ-LY-006 | Настройки лояльности | ✅ | `LoyaltyServiceTest.settingsOverrideDefaults`, `missingSettingsFallBackToDocumentedDefaults` |

## 18. Окно (2)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-UI-010 | Сохранение позиции/размера окна | ✅ | `JsonStateManagerTest` (оконный менеджер), `BaseUITest` |
| REQ-UI-011 | Ограничение размера границами экрана | 🟡 | `WindowStateManager.restoreWindowState`; ручная проверка при смене монитора |

## 19. CI и релиз (4)

| ID | Требование | Статус | Проверка |
|---|---|---|---|
| REQ-CI-001 | `build` с тестами и покрытием | ✅ | `.github/workflows/ci.yml`, локальный `./gradlew build` |
| REQ-CI-002 | Отдельная джоба UI-тестов | ✅ | `ui-test` + Xvfb |
| REQ-CI-003 | Мутации по `workflow_dispatch` | ✅ | джоба `mutation` |
| REQ-CI-004 | Релиз по тегу `v*` | ✅ | `.github/workflows/release.yml`, `jpackage` |

## 20. Отчётность (4)

| ID | Требование | Статус | Тесты |
|---|---|---|---|
| REQ-RP-001 | Сводный отчёт по заказам и выручке | ✅ | `StatisticsServiceTest` |
| REQ-RP-002 | Топ услуг и запчастей | ✅ | `StatisticsServiceTest` |
| REQ-RP-003 | PDF-экспорт отчёта (`ReportGenerator`) | ✅ | `ReportGeneratorTest` |
| REQ-RP-004 | Печать заказа-наряда в PDF | ✅ | `PrintOrderDialogTest` |

## Сводная статистика

| Категория | Требований | Покрыто |
|---|---:|---:|
| Клиенты и автомобили | 8 | 8 |
| Услуги | 6 | 6 |
| Запчасти и склад | 7 | 7 |
| Заказы | 10 | 10 |
| Статусы заказов | 3 | 3 |
| Записи | 10 | 10 |
| Статусы записей | 4 | 4 |
| Валидация | 8 | 8 |
| База данных | 12 | 12 |
| DataStore | 10 | 10 |
| Расходники ТО | 3 | 3 |
| Резервное копирование | 6 | 6 |
| Состояние таблиц | 6 | 6 |
| Темы | 5 | 5 |
| Календарь (визуально) | 4 | 4 |
| Таблицы («зебра») | 4 | 4 |
| Лояльность | 6 | 6 |
| Окно | 2 | 2 |
| CI и релиз | 4 | 4 |
| Отчётность | 4 | 4 |
| **ВСЕГО** | **122** | **122** |

## Summary

- Требований: **122**.
- Покрыто автотестами полностью: **117**; частично UI/ручными сценариями: **5**
  (переключатель календаря, легенда/счётчик, видимость записи, ручной ввод
  модели ТО, ограничение окна — отмечены 🟡).
- Юнит/интеграционных тестов: **571**; UI-тестов: **51**.
- Соотношение тестов к требованиям: **5,1 : 1** (622 / 122).

---

**Матрица составлена:** 30.05.2026
**Актуализирована под 1.2.0:** 18.11.2026
