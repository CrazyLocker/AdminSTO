import sqlite3

conn = sqlite3.connect('autoservice.db')
cursor = conn.cursor()

# Count orders per month from generated file
with open('docs/add_closed_orders.sql', 'r') as f:
    content = f.read()

import re
orders = re.findall(r"ZAK-(\d+)/(\d+)/26-\d+", content)
months = {}
for day, month in orders:
    months[month] = months.get(month, 0) + 1

print("Orders per month (generated):")
for m in sorted(months.keys()):
    print(f"  Month {m}: {months[m]} orders")

print(f"\nTotal orders: {len(orders)}")

conn.close()
