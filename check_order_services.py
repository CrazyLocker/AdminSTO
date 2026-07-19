import sqlite3

conn = sqlite3.connect('autoservice.db')
c = conn.cursor()

print("=== Заказы ===")
c.execute('SELECT id, status, total, created_date FROM orders')
orders = c.fetchall()
print(f'Всего заказов: {len(orders)}')
for row in orders:
    print(f'  {row[0]} | {row[1]} | {row[2]} | {row[3]}')

print("\n=== Услуги в order_services ===")
c.execute('SELECT order_id, service_name, price, service_id FROM order_services')
rows = c.fetchall()
print(f'Всего записей в order_services: {len(rows)}')
print('\nЗаписи:')
for row in rows:
    print(f'  order_id={row[0]}, service_name={row[1]}, price={row[2]}, service_id={row[3]}')

print("\n=== Группировка по order_id ===")
c.execute('''
    SELECT order_id, COUNT(*) as count, GROUP_CONCAT(service_name, ' | ') as services
    FROM order_services
    GROUP BY order_id
''')
for row in c.fetchall():
    print(f'  {row[0]}: {row[1]} услуг - {row[2]}')

conn.close()
