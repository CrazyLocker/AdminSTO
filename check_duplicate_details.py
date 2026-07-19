import sqlite3

conn = sqlite3.connect('autoservice.db')
c = conn.cursor()

print("=== Детальная проверка заказа ZAK-18/07/26-0001 ===")

# Получаем данные из order_services
c.execute('SELECT rowid, order_id, service_name, price, service_id FROM order_services WHERE order_id = "ZAK-18/07/26-0001"')
rows = c.fetchall()
print(f'\nЗаписей в order_services для заказа ZAK-18/07/26-0001: {len(rows)}')
for row in rows:
    print(f'  rowid={row[0]}, order_id={row[1]}, service_name={row[2]}, price={row[3]}, service_id={row[4]}')

# Получаем данные из orders
c.execute('SELECT id, client_id, status, total, created_date, closed_date, notes FROM orders WHERE id = "ZAK-18/07/26-0001"')
order = c.fetchone()
print(f'\nДанные заказа:')
print(f'  id={order[0]}, client_id={order[1]}, status={order[2]}, total={order[3]}, created_date={order[4]}')

# Проверяем, есть ли дубликаты в order_services по service_name
c.execute('''
    SELECT service_name, COUNT(*) as cnt, GROUP_CONCAT(rowid) as rowids
    FROM order_services
    WHERE order_id = "ZAK-18/07/26-0001"
    GROUP BY service_name
    HAVING COUNT(*) > 1
''')
duplicates = c.fetchall()
if duplicates:
    print(f'\nДубликаты найдены:')
    for d in duplicates:
        print(f'  service_name={d[0]}, count={d[1]}, rowids={d[2]}')
else:
    print('\nДубликатов не найдено')

conn.close()
