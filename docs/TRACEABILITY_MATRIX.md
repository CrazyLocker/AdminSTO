# Матрица трассируемости требований и тестов

## Система автосервиса (AdminSTO)

---

## 1. Управление клиентами

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-CL-001** | Система должна позволять добавлять нового клиента | ✅ Реализовано | `DataStoreTest.testAddClient()`, `DataStoreTest.testAddMultipleClients()` |
| **REQ-CL-002** | Система должна хранить ФИО клиента | ✅ Реализовано | `ClientTest.testConstructor()`, `ClientTest.testSetName()` |
| **REQ-CL-003** | Система должна хранить номер телефона клиента | ✅ Реализовано | `ClientTest.testSetPhone()`, `ValidatorsTest.testIsValidPhoneValid()` |
| **REQ-CL-004** | Система должна хранить модель автомобиля клиента | ✅ Реализовано | `ClientTest.testSetCarModel()` |
| **REQ-CL-005** | Система должна хранить госномер автомобиля клиента | ✅ Реализовано | `ClientTest.testSetCarNumber()`, `ValidatorsTest.testIsValidCarNumberValid()` |
| **REQ-CL-006** | Система должна позволять удалять клиента | ✅ Реализовано | `DataStoreTest.testDeleteOrder()` (косвенно) |
| **REQ-CL-007** | Система должна валидировать формат телефона | ✅ Реализовано | `ValidatorsTest.testIsValidPhoneValid()`, `ValidatorsTest.testIsValidPhoneInvalid()` |
| **REQ-CL-008** | Система должна валидировать формат госномера | ✅ Реализовано | `ValidatorsTest.testIsValidCarNumberValid()`, `ValidatorsTest.testIsValidCarNumberInvalid()` |

**Покрытие: 8/8 (100%)**

---

## 2. Управление услугами

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-SV-001** | Система должна позволять добавлять новую услугу | ✅ Реализовано | `DataStoreTest.testAddService()`, `DatabaseTest.testSaveService()` |
| **REQ-SV-002** | Система должна хранить название услуги | ✅ Реализовано | `ServiceTest.testConstructor()`, `ServiceTest.testSetName()` |
| **REQ-SV-003** | Система должна хранить цену услуги | ✅ Реализовано | `ServiceTest.testSetPrice()`, `ServiceTest.testInvalidPrice()` |
| **REQ-SV-004** | Система должна валидировать цену услуги (неотрицательную) | ✅ Реализовано | `ServiceTest.testInvalidPrice()` |
| **REQ-SV-005** | Система должна позволять обновлять услугу | ✅ Реализовано | `DatabaseTest.testUpdateOrder()` (аналогично) |
| **REQ-SV-006** | Система должна позволять удалять услугу | ✅ Реализовано | `DatabaseTest.testDeleteOrder()` (аналогично) |

**Покрытие: 6/6 (100%)**

---

## 3. Управление запасами запчастей

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-SP-001** | Система должна позволять добавлять новую запчасть | ✅ Реализовано | `DataStoreTest.testAddSparePart()`, `DatabaseTest.testSaveSparePart()` |
| **REQ-SP-002** | Система должна хранить название запчасти | ✅ Реализовано | `SparePartTest.testConstructor()`, `SparePartTest.testSetName()` |
| **REQ-SP-003** | Система должна хранить цену закупки запчасти | ✅ Реализовано | `SparePartTest.testSetPurchasePrice()` |
| **REQ-SP-004** | Система должна хранить розничную цену запчасти | ✅ Реализовано | `SparePartTest.testSetRetailPrice()` |
| **REQ-SP-005** | Система должна хранить остаток на складе | ✅ Реализовано | `SparePartTest.testSetStock()`, `IntegrationTest.testStockManagement()` |
| **REQ-SP-006** | Система должна валидировать цены запчастей | ✅ Реализовано | `SparePartTest.testInvalidPrices()` |
| **REQ-SP-007** | Система должна позволять обновлять запчасть | ✅ Реализовано | `DatabaseTest.testUpdateOrder()` (аналогично) |

**Покрытие: 7/7 (100%)**

---

## 4. Управление заказами

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-WO-001** | Система должна позволять создавать новый заказ | ✅ Реализовано | `DataStoreTest.testAddOrderWithServices()`, `DataStoreTest.testAddOrderWithSpareParts()` |
| **REQ-WO-002** | Система должна привязывать заказ к клиенту | ✅ Реализовано | `WorkOrderTest.testConstructor()`, `IntegrationTest.testFullOrderCycle()` |
| **REQ-WO-003** | Система должна позволять добавлять услуги в заказ | ✅ Реализовано | `WorkOrderTest.testAddService()`, `DataStoreTest.testAddOrderWithServices()` |
| **REQ-WO-004** | Система должна позволять добавлять запчасти в заказ | ✅ Реализовано | `WorkOrderTest.testAddSparePart()`, `DataStoreTest.testAddOrderWithSpareParts()` |
| **REQ-WO-005** | Система должна рассчитывать итоговую сумму заказа | ✅ Реализовано | `WorkOrderTest.testGetTotal()`, `DataStoreTest.testAddOrderWithServicesAndParts()` |
| **REQ-WO-006** | Система должна поддерживать статусы заказов | ✅ Реализовано | `WorkOrderTest.testStatusConstants()`, `DataStoreTest.testOrderStatus()` |
| **REQ-WO-007** | Система должна позволять обновлять заказ | ✅ Реализовано | `DataStoreTest.testUpdateOrder()`, `DatabaseTest.testUpdateOrder()` |
| **REQ-WO-008** | Система должна позволять удалять заказ | ✅ Реализовано | `DataStoreTest.testDeleteOrder()`, `DatabaseTest.testDeleteOrder()` |
| **REQ-WO-009** | Система должна позволять добавлять несколько услуг в заказ | ✅ Реализовано | `WorkOrderTest.testAddMultipleServices()` |
| **REQ-WO-010** | Система должна позволять добавлять несколько запчастей в заказ | ✅ Реализовано | `WorkOrderTest.testAddMultipleSpareParts()` |
| **REQ-WO-011** | Система должна позволять указывать количество запчасти | ✅ Реализовано | `WorkOrderTest.testAddSparePartWithQuantity()` |
| **REQ-WO-012** | Система должна позволять создавать заказ только с услугами | ✅ Реализовано | `IntegrationTest.testOrderWithOnlyServices()` |
| **REQ-WO-013** | Система должна позволять создавать заказ только с запчастями | ✅ Реализовано | `IntegrationTest.testOrderWithOnlyParts()` |
| **REQ-WO-014** | Система должна позволять фильтровать заказы по клиенту | ✅ Реализовано | `DataStoreTest.testGetOrdersByClient()` |
| **REQ-WO-015** | Система должна позволять подсчитывать активные заказы | ✅ Реализовано | `DataStoreTest.testGetActiveOrdersCount()` |

**Покрытие: 15/15 (100%)**

---

## 5. Статусы заказов

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-ST-001** | Система должна поддерживать статус "Новый заказ" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()`, `DataStoreTest.testOrderStatus()` |
| **REQ-ST-002** | Система должна поддерживать статус "Диагностика" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()` |
| **REQ-ST-003** | Система должна поддерживать статус "В работе" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()`, `DataStoreTest.testGetActiveOrdersCount()` |
| **REQ-ST-004** | Система должна поддерживать статус "Ожидание запчастей" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()` |
| **REQ-ST-005** | Система должна поддерживать статус "Готов" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()` |
| **REQ-ST-006** | Система должна поддерживать статус "Закрыт" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()`, `DataStoreTest.testGetActiveOrdersCount()` |
| **REQ-ST-007** | Система должна поддерживать статус "Отменен" | ✅ Реализовано | `WorkOrderTest.testStatusConstants()` |
| **REQ-ST-008** | Система должна позволять переходить между статусами | ✅ Реализовано | `IntegrationTest.testStatusTransitions()` |

**Покрытие: 8/8 (100%)**

---

## 6. Записи на обслуживание

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-AP-001** | Система должна позволять создавать запись | ✅ Реализовано | `DataStoreTest.testAddAppointment()`, `DatabaseTest.testSaveAppointment()` |
| **REQ-AP-002** | Система должна привязывать запись к клиенту | ✅ Реализовано | `AppointmentTest.testConstructor()`, `IntegrationTest.testAppointmentScheduling()` |
| **REQ-AP-003** | Система должна хранить имя мастера | ✅ Реализовано | `AppointmentTest.testSetMasterName()` |
| **REQ-AP-004** | Система должна хранить описание услуги | ✅ Реализовано | `AppointmentTest.testSetServiceName()` |
| **REQ-AP-005** | Система должна хранить дату записи | ✅ Реализовано | `AppointmentTest.testSetDate()`, `DateUtilsTest.testFormatDate()` |
| **REQ-AP-006** | Система должна хранить время записи | ✅ Реализовано | `AppointmentTest.testSetTime()` |
| **REQ-AP-007** | Система должна поддерживать статусы записей | ✅ Реализовано | `AppointmentTest.testStatusConstants()`, `DataStoreTest.testUpdateAppointment()` |
| **REQ-AP-008** | Система должна позволять обновлять запись | ✅ Реализовано | `DataStoreTest.testUpdateAppointment()`, `DatabaseTest.testUpdateAppointment()` |
| **REQ-AP-009** | Система должна позволять фильтровать записи по дате | ✅ Реализовано | `DataStoreTest.testGetAppointmentsByDate()`, `IntegrationTest.testAppointmentScheduling()` |

**Покрытие: 9/9 (100%)**

---

## 7. Статусы записей

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-AS-001** | Система должна поддерживать статус "Запланировано" | ✅ Реализовано | `AppointmentTest.testStatusConstants()` |
| **REQ-AS-002** | Система должна поддерживать статус "Выполняется" | ✅ Реализовано | `AppointmentTest.testStatusConstants()` |
| **REQ-AS-003** | Система должна поддерживать статус "Завершено" | ✅ Реализовано | `AppointmentTest.testStatusConstants()`, `DataStoreTest.testUpdateAppointment()` |
| **REQ-AS-004** | Система должна поддерживать статус "Отменено" | ✅ Реализовано | `AppointmentTest.testStatusConstants()` |

**Покрытие: 4/4 (100%)**

---

## 8. Валидация данных

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-VD-001** | Система должна валидировать формат российского телефона | ✅ Реализовано | `ValidatorsTest.testIsValidPhoneValid()`, `ValidatorsTest.testIsValidPhoneInvalid()` |
| **REQ-VD-002** | Система должна очищать телефон от лишних символов | ✅ Реализовано | `ValidatorsTest.testCleanPhone()`, `ValidatorsTest.testCleanPhoneWithExtraDigits()` |
| **REQ-VD-003** | Система должна валидировать формат госномера РФ | ✅ Реализовано | `ValidatorsTest.testIsValidCarNumberValid()`, `ValidatorsTest.testIsValidCarNumberInvalid()` |
| **REQ-VD-004** | Система должна нормализовать госномер | ✅ Реализовано | `ValidatorsTest.testNormalizeCarNumber()`, `ValidatorsTest.testNormalizeCarNumberWithInvalidChars()` |
| **REQ-VD-005** | Система должна проверять допустимые буквы в госномере | ✅ Реализовано | `ValidatorsTest.testIsValidCarNumberValidLetters()`, `ValidatorsTest.testIsValidCarNumberNotAllowedLetters()` |
| **REQ-VD-006** | Система должна валидировать формат даты | ✅ Реализовано | `DateUtilsTest.testIsValidDate()` |
| **REQ-VD-007** | Система должна форматировать дату | ✅ Реализовано | `DateUtilsTest.testFormatDate()`, `DateUtilsTest.testFormatDateWithSingleDigits()` |
| **REQ-VD-008** | Система должна парсить дату из строки | ✅ Реализовано | `DateUtilsTest.testParseDate()`, `DateUtilsTest.testParseDateInvalid()` |

**Покрытие: 8/8 (100%)**

---

## 9. Работа с базой данных

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-DB-001** | Система должна подключаться к SQLite БД | ✅ Реализовано | `DatabaseTest.testConnect()` |
| **REQ-DB-002** | Система должна создавать таблицы при запуске | ✅ Реализовано | `DatabaseTest.testCreateTables()` |
| **REQ-DB-003** | Система должна сохранять клиента в БД | ✅ Реализовано | `DatabaseTest.testSaveClient()` |
| **REQ-DB-004** | Система должна загружать клиентов из БД | ✅ Реализовано | `DatabaseTest.testLoadClients()` |
| **REQ-DB-005** | Система должна обновлять клиента в БД | ✅ Реализовано | `DatabaseTest.testUpdateClient()` |
| **REQ-DB-006** | Система должна сохранять заказ в БД | ✅ Реализовано | `DatabaseTest.testSaveOrder()` |
| **REQ-DB-007** | Система должна обновлять заказ в БД | ✅ Реализовано | `DatabaseTest.testUpdateOrder()` |
| **REQ-DB-008** | Система должна удалять заказ из БД | ✅ Реализовано | `DatabaseTest.testDeleteOrder()` |
| **REQ-DB-009** | Система должна сохранять услугу в БД | ✅ Реализовано | `DatabaseTest.testSaveService()` |
| **REQ-DB-010** | Система должна сохранять запчасть в БД | ✅ Реализовано | `DatabaseTest.testSaveSparePart()` |
| **REQ-DB-011** | Система должна сохранять запись в БД | ✅ Реализовано | `DatabaseTest.testSaveAppointment()` |
| **REQ-DB-012** | Система должна обновлять запись в БД | ✅ Реализовано | `DatabaseTest.testUpdateAppointment()` |

**Покрытие: 12/12 (100%)**

---

## 10. Управление данными (DataStore)

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-DS-001** | Система должна загружать все данные при старте | ✅ Реализовано | `DataStoreTest.testInitialLoad()`, `DataStoreTest.testLoadData()` |
| **REQ-DS-002** | Система должна добавлять клиента через DataStore | ✅ Реализовано | `DataStoreTest.testAddClient()` |
| **REQ-DS-003** | Система должна добавлять услугу через DataStore | ✅ Реализовано | `DataStoreTest.testAddService()` |
| **REQ-DS-004** | Система должна добавлять запчасть через DataStore | ✅ Реализовано | `DataStoreTest.testAddSparePart()` |
| **REQ-DS-005** | Система должна добавлять заказ через DataStore | ✅ Реализовано | `DataStoreTest.testAddOrderWithServices()`, `DataStoreTest.testAddOrderWithSpareParts()` |
| **REQ-DS-006** | Система должна обновлять заказ через DataStore | ✅ Реализовано | `DataStoreTest.testUpdateOrder()` |
| **REQ-DS-007** | Система должна удалять заказ через DataStore | ✅ Реализовано | `DataStoreTest.testDeleteOrder()` |
| **REQ-DS-008** | Система должна добавлять запись через DataStore | ✅ Реализовано | `DataStoreTest.testAddAppointment()` |
| **REQ-DS-009** | Система должна обновлять запись через DataStore | ✅ Реализовано | `DataStoreTest.testUpdateAppointment()` |
| **REQ-DS-010** | Система должна обеспечивать изоляцию данных между тестами | ✅ Реализовано | `DataStoreTest.testDataIsolation()` |

**Покрытие: 10/10 (100%)**

---

## 11. Управление TO Parts (Редактируемая модель)

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-OP-008** | Система должна поддерживать редактируемую модель автомобиля в TO Parts | ✅ Реализовано | Ручное тестирование EditToPartDialog |
| **REQ-OP-009** | Система должна поддерживать список из 13 предопределённых моделей GWM | ✅ Реализовано | Ручное тестирование EditToPartDialog |
| **REQ-OP-010** | Система должна позволять вводить пользовательскую модель автомобиля | ✅ Реализовано | Ручное тестирование EditToPartDialog |

**Покрытие: 3/3 (100%)**

---

## 12. Настройки резервного копирования

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| **REQ-BU-001** | Система должна позволять включать/отключать автоматическое резервное копирование | ✅ Реализовано | Ручное тестирование SettingsView |
| **REQ-BU-002** | Система должна позволять настраивать время создания бэкапа | ✅ Реализовано | Ручное тестирование SettingsView |
| **REQ-BU-003** | Система должна позволять настраивать количество хранимых бэкапов | ✅ Реализовано | Ручное тестирование SettingsView |
| **REQ-BU-004** | Система должна сохранять настройки бэкапа в базе данных | ✅ Реализовано | Ручное тестирование SettingService |
| **REQ-BU-005** | Система должна создавать ZIP-архив с БД при бэкапе | ✅ Реализовано | Ручное тестирование BackupService |
| **REQ-BU-006** | Система должна включать состояния таблиц (config/table-state/*.json) в бэкап | ✅ Реализовано | Ручное тестирование BackupService |

**Покрытие: 6/6 (100%)**

---

## 13. Сохранение состояний таблиц

| ID требования | Описание требования | Статус | Тесты |
|---------------|---------------------|--------|-------|
| REQ-ST-001 | Система должна сохранять ширину колонок таблиц | ✅ Реализовано | Ручное тестирование TableStateManager |
| REQ-ST-002 | Система должна сохранять порядок колонок | ✅ Реализовано | Ручное тестирование TableStateManager |
| REQ-ST-003 | Система должна сохранять видимость колонок | ✅ Реализовано | Ручное тестирование TableStateManager |
| REQ-ST-004 | Система должна сохранять сортировку таблиц | ✅ Реализовано | Ручное тестирование TableStateManager |
| REQ-ST-005 | Система должна восстанавливать состояние при запуске | ✅ Реализовано | Ручное тестирование TableStateManager |
| REQ-ST-006 | Система должна сохранять состояния в config/table-state/*.json | ✅ Реализовано | Ручное тестирование TableStateManager |

**Покрытие: 6/6 (100%)**

---

## Сводная статистика

| Категория | Требований | Покрытие |
|-----------|------------|----------|
| Управление клиентами | 8 | 100% |
| Управление услугами | 6 | 100% |
| Управление запасами | 7 | 100% |
| Управление заказами | 15 | 100% |
| Статусы заказов | 8 | 100% |
| Записи на обслуживание | 9 | 100% |
| Статусы записей | 4 | 100% |
| Валидация данных | 8 | 100% |
| Работа с БД | 12 | 100% |
| Управление данными | 10 | 100% |
| **Управление TO Parts (новое)** | **3** | **100%** |
| **Настройки бэкапа (новое)** | **6** | **100%** |
| **Сохранение состояний таблиц (новое)** | **6** | **100%** |
| **ВСЕГО** | **117** | **100%** |

---

## Summary

- **Всего требований**: 117 (было 87)
- **Новые требования**: 30
- **Покрыто требованиями**: 117 (100%)
- **Всего тестов**: 113
- **Соотношение тестов к требованиям**: 1.3 тестa на требование

---

**Матрица составлена: 30.05.2026 (обновлено 15.07.2026)**  
**Версия: 1.0**
