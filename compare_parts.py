import sqlite3
import csv

# Читаем БД
conn = sqlite3.connect('autoservice.db')
cursor = conn.execute("SELECT id, name, part_number, manufacturer, compatible_models, retail_price, stock FROM spare_parts ORDER BY name")
db_rows = cursor.fetchall()
conn.close()

db_names = set()
for r in db_rows:
    db_names.add(r[1])

print(f"=== СУЩЕСТВУЮЩИЕ ЗАПЧАСТИ В БД ({len(db_names)}) ===")
for r in db_rows:
    print(f"  id={r[0]:2d} | name={r[1]:40s} | part={r[2]:20s} | manuf={r[3]:15s} | models={r[4]:30s} | price={r[5]:10.1f} | stock={r[6]}")

# Читаем CSV
csv_names = []
with open('GWM_deepseek.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f, delimiter=';')
    for row in reader:
        csv_names.append(row['name'].strip())

print(f"\n=== ЗАПЧАСТИ В CSV ({len(csv_names)}) ===")
for name in csv_names:
    print(f"  {name}")

# Находим недостающие
print("\n=== НЕДОСТАЮЩИЕ ЗАПЧАСТИ (в CSV, но нет в БД) ===")
missing = 0
for name in csv_names:
    if name not in db_names:
        missing += 1
        print(f"  [{missing}] {name}")

print(f"\n=== УЖЕ ЕСТЬ В БД ({len(csv_names) - missing}) ===")
found = 0
for name in csv_names:
    if name in db_names:
        found += 1
        print(f"  [{found}] {name}")

print(f"\n--- ИТОГО: {missing} недостающих, {found} уже есть в БД ---")
