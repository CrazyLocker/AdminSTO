import sqlite3

conn = sqlite3.connect('autoservice.db')
cursor = conn.execute("SELECT id, name, part_number, manufacturer, compatible_models, retail_price, stock FROM spare_parts ORDER BY name")
rows = cursor.fetchall()
conn.close()

print("=== СУЩЕСТВУЮЩИЕ ЗАПЧАСТИ В БД ===")
for r in rows:
    print(f"  id={r[0]} | name={r[1]} | part={r[2]} | manuf={r[3]} | models={r[4]} | price={r[5]} | stock={r[6]}")

print(f"\nВсего в БД: {len(rows)} запчастей")
