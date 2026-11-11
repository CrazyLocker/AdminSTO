# Анализ импорта данных из старой базы

## Файлы в `Import_Export/`

### 1. `Clients.xml` — ✅ МОЖНО ИМПОРТИРОВАТЬ

**Формат XML:**
```xml
<client>
    <lastName>Петров</lastName>
    <name>Петр</name>
    <phone>+79001234567</phone>
    <carModel>Haval Dargo</carModel>
    <carNumber>А123ВС163</carNumber>
    <lastRepairDate>2026-05-21</lastRepairDate>
</client>
```

**Код импорта** (`ImportService.importClientsFromXml`):
- Читает теги: `lastName`, `name`, `phone`, `carModel`, `carNumber`, `lastRepairDate`
- Создаёт `Client` через конструктор
- Проверяет дубли по телефону
- Сохраняет через `DataStore.addClient()`

**Совместимость:** ✅ Полная. Теги XML соответствуют полям класса `Client`.

**Нюансы:**
- Поле `phone` сохраняется в формате `+79001234567` (без пробелов) — это корректно
- Поля `carModel` и `carNumber` сохраняются в legacy-полях клиента
- **Автомобили НЕ импортируются** в таблицу `client_cars` (в старой БД её не было)
- Данные `carModel`/`carNumber` будут видны в таблице клиентов, но не создадут записи в `client_cars`

---

### 2. `Services.xml` — ✅ МОЖНО ИМПОРТИРОВАТЬ

**Формат XML:**
```xml
<service>
    <name>Замена масла ДВС</name>
    <duration>60</duration>
    <partNumber>SRV-001</partNumber>
    <price>1500.0</price>
</service>
```

**Код импорта** (`ImportService.importServicesFromXml`):
- Читает теги: `name`, `duration`, `partNumber`, `price`
- Проверяет дубли по названию
- Сохраняет через `DataStore.addService()`

**Совместимость:** ✅ Полная. Теги XML соответствуют полям класса `Service`.

**Нюансы:**
- Поля `category_id`, `oil_volume`, `uses_oil`, `spare_part_name`, `spare_part_quantity` не импортируются (заполнятся дефолтными значениями)
- Дубли по названию пропускаются

---

### 3. `SpareParts.xml` — ⚠️ МОЖНО ИМПОРТИРОВАТЬ, НО С ОГРАНИЧЕНИЯМИ

**Формат XML:**
```xml
<part>
    <name>Аккумулятор 60Ah</name>
    <partNumber>BAT-60-01</partNumber>
    <manufacturer>Varta</manufacturer>
    <retailPrice>8900.0</retailPrice>
    <purchasePrice>5500.0</purchasePrice>
    <stock>10.0</stock>
    <location>Стеллаж Е1</location>
    <compatibleModels>Haval Jolion;Haval F7</compatibleModels>
</part>
```

**Код импорта** (`ImportService.importFromXml`):
- Читает теги: `name`, `partNumber`, `manufacturer`, `retailPrice`, `purchasePrice`, `stock`, `location`, `compatibleModels`, `unitType`
- Ищет дубли по `partNumber`
- Если запчасть есть — обновляет `stock`, `retailPrice`, `purchasePrice`, `manufacturer`
- Если нет — создаёт новую

**Совместимость:** ✅ Работает, но есть нюансы.

**Нюансы:**
- Поле `stock` в XML — `10.0` (double), но в коде `parseIntSafe` — будет приведено к `int` (10)
- Поле `unitType` не импортируется (по умолчанию "шт")
- Поле `note` не импортируется
- Поле `min_stock` не импортируется

---

### 4. CSV-файлы — ❌ НЕ ПОДДЕРЖИВАЮТСЯ

Файлы `GWM_deepseek.csv`, `GWM_deepseek_converted.csv`, `GWM_deepseek_uslugi.csv` **не поддерживаются** системой импорта. Код определяет только формат XML.

---

## Ключевые ограничения

### 1. Автомобили клиентов не импортируются
В старой БД не было таблицы `client_cars`. При импорте клиентов:
- Поля `carModel` и `carNumber` сохраняются в legacy-поля таблицы `clients`
- Записи в `client_cars` **НЕ создаются**
- Это значит, что у импортированных клиентов не будет автомобилей в списке

### 2. Нет импорта заказов, записей, связей
- Заказы (`orders`)
- Записи на приём (`appointments`)
- Связи услуг и запчастей (`service_spare_parts`)
- Списки запчастей для ТО (`service_spare_parts_lists`)
- Настройки (`app_settings`)
- TO-запчасти (`to_parts`)

### 3. Дублирование
- Клиенты проверяются по телефону (пропускаются дубли)
- Услуги проверяются по названию (пропускаются дубли)
- Запчасти проверяются по `partNumber` (обновляются, если есть)

---

## Рекомендации

### Для полного переноса данных:
1. Импортировать `Clients.xml` — клиенты появятся в справочнике
2. Импортировать `Services.xml` — услуги появятся в справочнике
3. Импортировать `SpareParts.xml` — запчасти появятся в справочнике
4. Автомобили клиентам придётся добавить вручную через редактирование клиента
5. Заказы и записи из старой БД не переносятся

### Если нужно добавить автомобили при импорте:
Можно расширить `ImportService.importClientsFromXml`, чтобы:
- При создании клиента также создавать запись в `client_cars`
- Использовать данные из `carModel` и `carNumber`
- Устанавливать `mileage = 0`

---

## Вывод

**Можно импортировать:**
- ✅ Клиенты (Clients.xml)
- ✅ Услуги (Services.xml)
- ✅ Запчасти (SpareParts.xml)

**Не импортируется:**
- ❌ Автомобили клиентов (в `client_cars`)
- ❌ Заказы
- ❌ Записи на приём
- ❌ CSV-файлы

**Для большинства сценариев импорта XML-файлов достаточно.** Автомобили и заказы придётся добавлять вручную.
