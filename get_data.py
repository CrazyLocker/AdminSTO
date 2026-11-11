import sqlite3

conn = sqlite3.connect('autoservice.db')
cursor = conn.cursor()

# Get services
cursor.execute("SELECT id, name, price FROM services LIMIT 20")
print("=== SERVICES ===")
for r in cursor.fetchall():
    print(r)

# Get spare parts
cursor.execute("SELECT id, name, retail_price FROM spare_parts LIMIT 20")
print("\n=== SPARE PARTS ===")
for r in cursor.fetchall():
    print(r)

# Get clients
cursor.execute("SELECT id, name, last_name, car_model, car_number FROM clients")
print("\n=== CLIENTS ===")
for r in cursor.fetchall():
    print(r)

conn.close()
