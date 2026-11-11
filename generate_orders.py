import sqlite3
import random
from datetime import date, timedelta

random.seed(123)

conn = sqlite3.connect('autoservice.db')
cursor = conn.cursor()

# Get data
cursor.execute("SELECT id, name, price FROM services")
services = cursor.fetchall()

cursor.execute("SELECT id, name, retail_price FROM spare_parts")
parts = cursor.fetchall()

cursor.execute("SELECT id, name, last_name, car_model, car_number FROM clients")
clients = cursor.fetchall()

# Get max order number
cursor.execute("SELECT id FROM orders ORDER BY id DESC LIMIT 1")
last_order = cursor.fetchone()[0]
# Parse last number: ZAK-DD/MM/YY-NNNN
last_num = int(last_order.split('-')[-1])

# Date range: 2026-01-01 to 2026-08-11
start = date(2026, 1, 1)
end = date(2026, 8, 11)

# Generate SQL
sql_lines = []
order_num = last_num + 1

# Generate all dates in range grouped by month
all_dates = []
current = start
while current <= end:
    all_dates.append(current)
    current += timedelta(days=1)

# Group by month
months_dict = {}
for d in all_dates:
    key = f"{d.month:02d}"
    if key not in months_dict:
        months_dict[key] = []
    months_dict[key].append(d)

for month_key in sorted(months_dict.keys()):
    dates_in_month = months_dict[month_key]
    num_orders = random.randint(15, 30)
    selected_dates = []
    for _ in range(num_orders):
        selected_dates.append(random.choice(dates_in_month))
    selected_dates.sort()
    
    for d in selected_dates:
        
        # Format: ZAK-DD/MM/YY-NNNN
        order_id = f"ZAK-{d.day:02d}/{d.month:02d}/{d.year % 100:02d}-{order_num:04d}"
        order_num += 1
        
        # Pick client
        client = random.choice(clients)
        client_id = client[0]
        car_model = client[3]
        car_number = client[4]
        
        # Pick 2-3 services
        num_services = random.randint(2, 3)
        selected_services = random.sample(services, min(num_services, len(services)))
        
        # Pick 3-4 parts
        num_parts = random.randint(3, 4)
        selected_parts = random.sample(parts, min(num_parts, len(parts)))
        
        # Calculate total
        total = sum(s[2] for s in selected_services)
        total += sum(p[2] * random.randint(1, 3) for p in selected_parts)
        
        # Insert into orders
        sql_lines.append(
            f"INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes, car_model, car_number) "
            f"VALUES ('{order_id}', {client_id}, 'Закрыт', {total}, '{d.isoformat()}', "
            f"{random.randint(30000, 200000)}, '{d.isoformat()}', '', '{car_model}', '{car_number}');"
        )
        
        # Insert into order_services
        for svc in selected_services:
            sql_lines.append(
                f"INSERT INTO order_services (order_id, service_name, price, service_id) "
                f"VALUES ('{order_id}', '{svc[1]}', {svc[2]}, {svc[0]});"
            )
        
        # Insert into order_parts
        for part in selected_parts:
            qty = random.randint(1, 3)
            sql_lines.append(
                f"INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) "
                f"VALUES ('{order_id}', '{part[1]}', {part[2]}, {qty}, {part[0]}, 'шт', 0);"
            )

# Write SQL file
with open('docs/add_closed_orders.sql', 'w', encoding='utf-8') as f:
    f.write("-- Скрипт для добавления закрытых заказов\n")
    f.write("-- Автогенерация из существующих данных БД\n\n")
    f.write("BEGIN TRANSACTION;\n\n")
    for line in sql_lines:
        f.write(line + "\n")
    f.write("\nCOMMIT;\n")

print(f"Generated {len(sql_lines)} SQL statements")
print(f"Orders: {order_num - last_num - 1}")
print(f"File: docs/add_closed_orders.sql")

conn.close()
