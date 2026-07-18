import sqlite3

conn = sqlite3.connect('autoservice.db')
conn.execute("PRAGMA foreign_keys = ON")

with open('docs/seed_30_orders.sql', 'r', encoding='utf-8') as f:
    sql = f.read()

conn.executescript(sql)
conn.commit()

c = conn.cursor()

c.execute("SELECT COUNT(*) FROM orders WHERE status = 'Закрыт'")
closed = c.fetchone()[0]
print(f"Закрытых заказов: {closed}")

c.execute("SELECT COUNT(*) FROM orders WHERE status = 'Новый'")
new_orders = c.fetchone()[0]
print(f"Новых заказов: {new_orders}")

c.execute("SELECT COUNT(*) FROM appointments")
appointments = c.fetchone()[0]
print(f"Записей в appointments: {appointments}")

c.execute("SELECT id, status, created_date, total FROM orders WHERE id LIKE 'ZAK-NEW-%' ORDER BY created_date")
print("\nВсе заказы ZAK-NEW:")
for row in c.fetchall():
    print(f"  {row[0]} | {row[1]} | {row[2]} | {row[3]}")

conn.close()
print("\nСкрипт выполнен успешно!")
