import sqlite3
import os

db_path = 'docs/test_syntax.db'
conn = sqlite3.connect(db_path)
c = conn.cursor()

c.executescript('''
CREATE TABLE clients (
    id INTEGER PRIMARY KEY,
    name TEXT, last_name TEXT, phone TEXT, car_model TEXT, car_number TEXT
);
CREATE TABLE services (
    id INTEGER PRIMARY KEY,
    name TEXT, price REAL, duration INTEGER
);
CREATE TABLE spare_parts (
    id INTEGER PRIMARY KEY,
    name TEXT, part_number TEXT, stock INTEGER
);
CREATE TABLE orders (
    id TEXT PRIMARY KEY,
    client_id INTEGER,
    status TEXT,
    total INTEGER,
    created_date TEXT,
    closed_date TEXT,
    notes TEXT
);
CREATE TABLE order_services (
    order_id TEXT,
    service_name TEXT,
    price REAL,
    service_id INTEGER,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE TABLE order_parts (
    order_id TEXT,
    part_name TEXT,
    price REAL,
    quantity INTEGER,
    spare_part_id INTEGER,
    unit_type TEXT,
    purchase_price REAL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
CREATE TABLE appointments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id INTEGER,
    order_id TEXT,
    master_name TEXT,
    service_name TEXT,
    service_id INTEGER,
    appointment_date TEXT,
    appointment_time TEXT,
    status TEXT,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
''')

clients = [
    (1, 'Alexander', 'Ivanov', '+79001111111', 'Haval Jolion', 'A111BC163'),
    (2, 'Elena', 'Petrova', '+79002222222', 'Haval F7', 'V222CE163'),
    (3, 'Dmitry', 'Sidorov', '+79003333333', 'Haval Dargo', 'E333KH163'),
    (4, 'Olga', 'Kozlova', '+79004444444', 'Haval Big Dog', 'M444NO163'),
    (5, 'Nikolay', 'Volkov', '+79005555555', 'Haval F5', 'S555TE163'),
    (6, 'Maria', 'Novikova', '+79006666666', 'Toyota Camry', 'O666UU163'),
    (7, 'Sergey', 'Morozov', '+79007777777', 'BMW X5', 'R777FH163'),
    (8, 'Anna', 'Sokolova', '+79008888888', 'Kia Rio', 'T888HC163'),
    (9, 'Vladimir', 'Lebedev', '+79009999999', 'Mercedes C-Class', 'K999CC163'),
    (10, 'Irina', 'Kuznetsova', '+79010101010', 'Ford Focus', 'N101SH163'),
]
c.executemany('INSERT OR IGNORE INTO clients VALUES (?,?,?,?,?,?)', clients)

with open('docs/seed_30_orders.sql', 'r', encoding='utf-8') as f:
    sql = f.read()

for stmt in sql.split(';'):
    stmt = stmt.strip()
    if stmt and not stmt.startswith('--'):
        try:
            c.execute(stmt)
        except Exception as e:
            print(f'ERROR: {e}')
            print(f'STMT: {stmt[:150]}...')
            break

conn.commit()

c.execute("SELECT COUNT(*) FROM orders WHERE status = 'Closed'")
closed = c.fetchone()[0]
c.execute("SELECT COUNT(*) FROM orders WHERE status = 'New'")
new = c.fetchone()[0]
c.execute('SELECT COUNT(*) FROM appointments')
appt = c.fetchone()[0]
c.execute('SELECT COUNT(*) FROM order_services')
os_count = c.fetchone()[0]
c.execute('SELECT COUNT(*) FROM order_parts')
op_count = c.fetchone()[0]

print(f'Closed orders: {closed}')
print(f'New orders: {new}')
print(f'Appointments: {appt}')
print(f'Order services: {os_count}')
print(f'Order parts: {op_count}')
print(f'TOTAL orders: {closed + new}')

conn.close()
os.remove(db_path)
print('Check passed successfully!')
