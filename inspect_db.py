import sqlite3

conn = sqlite3.connect('autoservice.db')
cursor = conn.cursor()

# Get all tables
cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
tables = [r[0] for r in cursor.fetchall()]
print("=== TABLES ===")
for t in tables:
    print(t)

# Get schema for each table
for t in tables:
    cursor.execute(f"PRAGMA table_info({t})")
    cols = cursor.fetchall()
    print(f"\n=== {t} ===")
    for c in cols:
        print(f"  {c[1]} ({c[2]}) default={c[4]}")

# Get sample data
for t in tables:
    cursor.execute(f"SELECT COUNT(*) FROM {t}")
    count = cursor.fetchone()[0]
    print(f"\n=== {t} rows: {count} ===")
    cursor.execute(f"SELECT * FROM {t} LIMIT 5")
    rows = cursor.fetchall()
    cursor.execute(f"PRAGMA table_info({t})")
    col_names = [c[1] for c in cursor.fetchall()]
    print(f"Columns: {col_names}")
    for row in rows:
        print(f"  {row}")

conn.close()
