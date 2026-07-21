import sqlite3

conn = sqlite3.connect('autoservice.db')
cursor = conn.cursor()

# Проверяем дубликаты
print('=== Проверка дубликатов ===')
cursor.execute('SELECT service_id, spare_part_id, COUNT(*) as cnt FROM service_spare_parts GROUP BY service_id, spare_part_id HAVING cnt > 1')
rows = cursor.fetchall()
print(f'Групп с дубликатами: {len(rows)}')
for r in rows:
    print(f'  service_id={r[0]}, spare_part_id={r[1]}: {r[2]} записей')

if not rows:
    print('Дубликатов нет')

# Список всех связей
print('\n=== Все связи ===')
cursor.execute('SELECT ssp.id, ssp.service_id, ssp.spare_part_id, ssp.quantity, ssp.unit_type FROM service_spare_parts ssp')
rows = cursor.fetchall()
print(f'Всего связей: {len(rows)}')
for r in rows:
    print(f'  ID={r[0]}, service_id={r[1]}, spare_part_id={r[2]}, qty={r[3]}, unit={r[4]}')

conn.close()
