import sqlite3
import csv

# Читаем БД
conn = sqlite3.connect('autoservice.db')
cursor = conn.execute("SELECT id, name, part_number, manufacturer, compatible_models, retail_price, stock FROM spare_parts ORDER BY name")
db_rows = cursor.fetchall()
conn.close()

db_names_lower = {}
for r in db_rows:
    db_names_lower[r[1].lower()] = r

print(f"=== СУЩЕСТВУЮЩИЕ ЗАПЧАСТИ В БД ({len(db_names_lower)}) ===")
for r in db_rows:
    print(f"  id={r[0]:2d} | name={r[1]:45s} | part={r[2]:25s} | manuf={r[3]:15s} | models={r[4]:30s} | price={r[5]:10.1f} | stock={r[6]}")

# Читаем CSV — разделитель запятая
csv_items = []
with open('GWM_deepseek.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    print(f"\n=== ЗАГОЛОВКИ CSV: {reader.fieldnames} ===")
    for row in reader:
        csv_items.append({
            'part_number': row.get('part_number', '').strip(),
            'name': row.get('name', '').strip(),
            'application': row.get('application', '').strip()
        })

print(f"\n=== ЗАПЧАСТИ В CSV ({len(csv_items)}) ===")
for item in csv_items:
    print(f"  part={item['part_number']:25s} | name={item['name']:50s} | app={item['application']}")

# Находим недостающие (по названию, с учётом регистра)
print("\n" + "="*80)
print("=== НЕДОСТАЮЩИЕ ЗАПЧАСТИ (в CSV, но нет в БД) ===")
missing = 0
for item in csv_items:
    name_lower = item['name'].lower()
    if name_lower not in db_names_lower:
        missing += 1
        print(f"\n  [{missing}] {item['name']}")
        print(f"      Артикул: {item['part_number']}")
        print(f"      Применение: {item['application']}")

print(f"\n" + "="*80)
print("=== УЖЕ ЕСТЬ В БД (совпадение по названию) ===")
found = 0
for item in csv_items:
    name_lower = item['name'].lower()
    if name_lower in db_names_lower:
        found += 1
        db_row = db_names_lower[name_lower]
        print(f"  [{found}] {item['name']}")
        print(f"      CSV: part={item['part_number']} app={item['application']}")
        print(f"      БД:  part={db_row[2]} models={db_row[4]} price={db_row[5]} stock={db_row[6]}")

print(f"\n--- ИТОГО: {missing} недостающих, {found} уже есть в БД ---")

# Сравнение параметров
print("\n" + "="*80)
print("=== СРАВНЕНИЕ ПОЛЕЙ CSV И БД ===")
print("""
CSV поля:
  - part_number    -> БД: part_number       ✅ сопоставлено
  - name           -> БД: name              ✅ сопоставлено
  - application    -> БД: compatible_models ✅ сопоставлено

Дополнительные поля в БД (нет в CSV):
  - manufacturer   -> производитель (❌ нет в CSV)
  - retail_price   -> розничная цена (❌ нет в CSV)
  - stock          -> остаток на складе (❌ нет в CSV)
  - purchase_price -> закупочная цена (❌ нет в CSV)
  - min_stock      -> минимальный остаток (❌ нет в CSV)
  - location       -> место хранения (❌ нет в CSV)
""")
