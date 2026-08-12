import sqlite3

conn = sqlite3.connect('autoservice.db')
cursor = conn.cursor()

# Get max order number
cursor.execute("SELECT id FROM orders ORDER BY id DESC LIMIT 1")
row = cursor.fetchone()
print(f"Last order: {row}")

# Count orders per month
cursor.execute("""
    SELECT strftime('%Y-%m', created_date) as month, COUNT(*) 
    FROM orders 
    GROUP BY month ORDER BY month
""")
print("\nOrders per month:")
for r in cursor.fetchall():
    print(f"  {r[0]}: {r[1]}")

conn.close()
