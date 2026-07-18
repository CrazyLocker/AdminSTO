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

# ТЕСТ: пробуем вставить заказ напрямую
try:
    c.execute("INSERT OR IGNORE INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)",
              ('TEST-001', 1, 'Closed', 1000, '01/01/2026', '02/01/2026', 'Test'))
    print(f'After direct INSERT: {c.execute("SELECT COUNT(*) FROM orders").fetchone()[0]} orders')
except Exception as e:
    print(f'Direct INSERT error: {e}')

with open('docs/seed_30_orders.sql', 'r', encoding='utf-8') as f:
    sql = f.read()

# Проверяем первый запрос
lines = sql.split('\n')
for i, line in enumerate(lines[:20]):
    print(f'{i+1}: {line}')

# Выполняем по частям — пробуем обычный INSERT
errors = []
stmt_count = 0
orders_inserted = 0

# Заменяем INSERT OR IGNORE на INSERT для отладки
debug_sql = sql.replace('INSERT OR IGNORE INTO', 'INSERT INTO')

# Показываем первые 5 операторов
parts = [p.strip() for p in debug_sql.split(';') if p.strip() and not p.strip().startswith('--')]
print(f'Total non-comment parts: {len(parts)}')

# Ищем все части с "orders"
print('\n--- Parts with "orders" ---')
for i, part in enumerate(parts):
    if 'INTO orders' in part and 'order_' not in part and 'appointment' not in part:
        print(f'\nPart {i+1}:')
        print(part[:200])
        print('---')

# Также покажем Part 0
print('\n--- Part 0 ---')
print(parts[0][:200] if parts else 'EMPTY')

# Теперь выполняем
for stmt in parts:
    stmt = stmt.strip()
    if stmt and not stmt.startswith('--'):
        stmt_count += 1
        is_orders = 'INTO orders (' in stmt and 'order_' not in stmt and 'appointment' not in stmt
        try:
            before = c.execute('SELECT COUNT(*) FROM orders').fetchone()[0]
            c.execute(stmt)
            after = c.execute('SELECT COUNT(*) FROM orders').fetchone()[0]
            if is_orders and after > before:
                orders_inserted += after - before
                print(f'Inserted {after-before} orders. Total: {after}')
        except Exception as e:
            errors.append(str(e))
            print(f'ERROR #{len(errors)}: {e}')
            print(f'STMT: {stmt[:200]}')
            if len(errors) <= 3:
                continue

print(f'\nTotal statements executed: {stmt_count}')
print(f'Orders inserted from script: {orders_inserted}')
if errors:
    print(f'Total errors: {len(errors)}')
else:
    print('No SQL errors!')

conn.commit()

# Проверка всех заказов
c.execute('SELECT id, status FROM orders')
all_orders = c.fetchall()
print(f'All orders: {all_orders}')
print(f'Total orders count: {len(all_orders)}')

c.execute("SELECT COUNT(*) FROM orders WHERE status = 'Закрыт'")
closed = c.fetchone()[0]
c.execute("SELECT COUNT(*) FROM orders WHERE status = 'Новый'")
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
