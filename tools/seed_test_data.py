#!/usr/bin/env python3
"""Наполнение базы AdminSTO тестовыми данными (клиенты, лояльность, заказы).

Скрипт генерирует SQL-файл с INSERT-ами и (по флагу ``--apply``) выполняет его
в указанной базе. Данные согласованы с логикой ``LoyaltyService``:

* звёзды ТО (``clients.maintenance_stars``) — по одной звезде за закрытый заказ
  с услугой категории ``MAINTENANCE``, если разница пробега с предыдущим ТО
  попадает в интервал 5 000–10 000 км (по умолчанию);
* ``clients.tuning_total`` — сумма позиций категории ``TUNING`` в закрытых
  заказах клиента (Stage 1 — от 30 000, Stage 2 — свыше 100 000,
  Stage 3 — свыше 300 000 руб.);
* ``orders.total`` — сумма строк ``order_services`` и ``order_parts``.

Примеры:

    python tools/seed_test_data.py --db T:/AdminSTO/autoservice.db            # только SQL
    python tools/seed_test_data.py --db T:/AdminSTO/autoservice.db --apply    # выполнить
    python tools/seed_test_data.py --db ... --apply --force                   # перезалить
"""

from __future__ import annotations

import argparse
import datetime as dt
import shutil
import sqlite3
import sys
from pathlib import Path

# ==================== СПРАВОЧНИКИ ====================

# (id, name, price, duration, part_number, category)
SERVICES = [
    (1, "Замена моторного масла", 2500.0, 40, "SRV-OIL-01", "MAINTENANCE"),
    (2, "Комплексное ТО", 8500.0, 180, "SRV-TO-02", "MAINTENANCE"),
    (3, "Замена воздушного фильтра", 900.0, 20, "SRV-AF-03", "MAINTENANCE"),
    (4, "Чип-тюнинг Stage 1", 45000.0, 120, "SRV-TUN-04", "TUNING"),
    (5, "Установка спортивного выхлопа", 120000.0, 240, "SRV-TUN-05", "TUNING"),
    (6, "Тюнинг подвески (занижение)", 250000.0, 480, "SRV-TUN-06", "TUNING"),
    (7, "Замена тормозных колодок", 3200.0, 90, "SRV-BRK-07", "REGULAR"),
    (8, "Диагностика подвески", 1500.0, 60, "SRV-DIAG-08", "REGULAR"),
    (9, "Ремонт кондиционера", 6500.0, 150, "SRV-AC-09", "REGULAR"),
]

# (id, name, part_number, manufacturer, retail, purchase, stock, min_stock, location, category, unit_type)
SPARE_PARTS = [
    (1, "Масло моторное 5W-40", "OIL-5W40", "Lukoil", 800.0, 600.0, 40.0, 10.0, "A-01", "MAINTENANCE", "л"),
    (2, "Масляный фильтр", "MF-210", "Mann", 850.0, 600.0, 25.0, 5.0, "A-02", "MAINTENANCE", "шт"),
    (3, "Воздушный фильтр", "AF-310", "Mann", 1200.0, 800.0, 18.0, 4.0, "A-03", "MAINTENANCE", "шт"),
    (4, "Тормозные колодки передние", "BP-150", "Brembo", 5200.0, 3800.0, 12.0, 2.0, "B-01", "REGULAR", "компл"),
    (5, "Свечи зажигания", "SP-77", "NGK", 900.0, 600.0, 60.0, 12.0, "B-02", "REGULAR", "шт"),
    (6, "Выхлопная система Sport", "EX-900", "Borla", 95000.0, 70000.0, 3.0, 1.0, "C-01", "TUNING", "компл"),
    (7, "Пружины заниженные", "SPR-45", "H&R", 45000.0, 30000.0, 5.0, 1.0, "C-02", "TUNING", "компл"),
]

# id: (имя, фамилия, телефон, звёзды ТО, накопленный тюнинг, [(модель, номер, пробег), ...])
CLIENTS = {
    2: ("Иван", "Петров", "+7 900 111-00-02", 0, 0.0,
        [("Lada Vesta", "А111АА163", 42_000)]),
    3: ("Мария", "Соколова", "+7 900 111-00-03", 0, 0.0,
        [("Kia Rio", "В222ВВ163", 18_000)]),
    4: ("Алексей", "Кузнецов", "+7 900 111-00-04", 3, 0.0,
        [("Toyota Camry", "С333СС163", 22_000)]),
    5: ("Дмитрий", "Волков", "+7 900 111-00-05", 3, 0.0,
        [("Skoda Octavia", "Е444ЕЕ163", 22_000),
         ("Renault Duster", "Е445ЕЕ163", 61_000)]),
    6: ("Анна", "Морозова", "+7 900 111-00-06", 4, 0.0,
        [("Hyundai Creta", "К555КК163", 29_000)]),
    7: ("Наталья", "Лебедева", "+7 900 111-00-07", 5, 0.0,
        [("Volkswagen Polo", "М666ММ163", 36_000)]),
    8: ("Сергей", "Титов", "+7 900 111-00-08", 0, 90_000.0,
        [("Audi A4", "Н777НН163", 88_000)]),
    9: ("Виктор", "Громов", "+7 900 111-00-09", 0, 210_000.0,
        [("BMW 320i", "О888ОО163", 74_000)]),
    10: ("Олег", "Белов", "+7 900 111-00-10", 0, 420_000.0,
         [("Mercedes-Benz C200", "Р999РР163", 52_000)]),
    11: ("Ирина", "Смирнова", "+7 900 111-00-11", 0, 0.0,
         [("Renault Logan", "Т111ТТ163", 95_000)]),
    12: ("Павел", "Козлов", "+7 900 111-00-12", 0, 0.0,
         [("Lada Granta", "У222УУ163", 12_000)]),
}

# Заказы: dict(client, status, created, closed, mileage, notes, services, parts)
#   services = [(service_id, name, price)]
#   parts    = [(name, price, quantity, spare_part_id, unit_type, purchase_price)]
ORDERS = []


def _order(client, status, created, closed, mileage, notes, services, parts=None):
    ORDERS.append({
        "client": client,
        "status": status,
        "created": created,
        "closed": closed,
        "mileage": mileage,
        "notes": notes,
        "services": services,
        "parts": parts or [],
    })


def _maintenance(client, seq, created, mileage):
    """Закрытый заказ ТО: пробег растёт на 6 000–8 000 км — каждый даёт звезду."""
    if seq % 3 == 1:
        services = [(1, "Замена моторного масла", 2500.0), (3, "Замена воздушного фильтра", 900.0)]
        parts = [("Масло моторное 5W-40", 800.0, 4, 1, "л", 600.0),
                 ("Масляный фильтр", 850.0, 1, 2, "шт", 600.0)]
        notes = "Плановое ТО: масло и фильтры"
    elif seq % 3 == 2:
        services = [(2, "Комплексное ТО", 8500.0)]
        parts = [("Масло моторное 5W-40", 800.0, 4, 1, "л", 600.0),
                 ("Масляный фильтр", 850.0, 1, 2, "шт", 600.0),
                 ("Воздушный фильтр", 1200.0, 1, 3, "шт", 800.0)]
        notes = "Комплексное ТО"
    else:
        services = [(1, "Замена моторного масла", 2500.0), (3, "Замена воздушного фильтра", 900.0)]
        parts = [("Масло моторное 5W-40", 800.0, 4, 1, "л", 600.0)]
        notes = "ТО по регламенту"
    _order(client, "Закрыт", created, created, mileage, notes, services, parts)


def build_orders():
    """Собрать заказы: ТО с растущим пробегом, тюнинг на разные суммы, обычные."""
    # --- Клиент 4: 3 звезды ТО (пробег 8 000 → 15 000 → 22 000) ---
    _maintenance(4, 1, "12/07/2026", 8_000)
    _maintenance(4, 2, "20/09/2026", 15_000)
    _maintenance(4, 3, "05/11/2026", 22_000)

    # --- Клиент 5: 3 звезды ТО ---
    _maintenance(5, 1, "18/07/2026", 8_000)
    _maintenance(5, 2, "26/09/2026", 15_000)
    _maintenance(5, 3, "08/11/2026", 22_000)

    # --- Клиент 6: 4 звезды ТО (8 000 → 29 000) ---
    _maintenance(6, 1, "05/06/2026", 8_000)
    _maintenance(6, 2, "10/08/2026", 15_000)
    _maintenance(6, 3, "14/10/2026", 22_000)
    _maintenance(6, 4, "12/11/2026", 29_000)

    # --- Клиент 7: 5 звёзд ТО, максимум (8 000 → 36 000) ---
    _maintenance(7, 1, "20/04/2026", 8_000)
    _maintenance(7, 2, "03/07/2026", 15_000)
    _maintenance(7, 3, "19/09/2026", 22_000)
    _maintenance(7, 4, "24/10/2026", 29_000)
    _maintenance(7, 5, "10/11/2026", 36_000)

    # --- Клиент 8: тюнинг на 90 000 руб. → Stage 1 ---
    _order(8, "Закрыт", "15/08/2026", "17/08/2026", 61_000,
           "Чип-тюнинг и пружины",
           [(4, "Чип-тюнинг Stage 1", 45_000.0)],
           [("Пружины заниженные", 45_000.0, 1, 7, "компл", 30_000.0)])

    # --- Клиент 9: тюнинг на 210 000 руб. → Stage 2 ---
    _order(9, "Закрыт", "11/09/2026", "14/09/2026", 58_000,
           "Выхлоп и чип-тюнинг",
           [(5, "Установка спортивного выхлопа", 120_000.0),
            (4, "Чип-тюнинг Stage 1", 45_000.0)],
           [("Пружины заниженные", 45_000.0, 1, 7, "компл", 30_000.0)])

    # --- Клиент 10: тюнинг на 420 000 руб. → Stage 3 ---
    _order(10, "Закрыт", "02/10/2026", "09/10/2026", 47_000,
           "Комплексный тюнинг подвески и выхлопа",
           [(6, "Тюнинг подвески (занижение)", 250_000.0),
            (5, "Установка спортивного выхлопа", 120_000.0),
            (4, "Чип-тюнинг Stage 1", 50_000.0)])

    # --- Клиент 11: обычный закрытый заказ (лояльность не меняет) ---
    _order(11, "Закрыт", "21/10/2026", "21/10/2026", 95_000,
           "Тормоза и диагностика",
           [(7, "Замена тормозных колодок", 3200.0), (8, "Диагностика подвески", 1500.0)],
           [("Тормозные колодки передние", 5200.0, 1, 4, "компл", 3800.0)])

    # --- Клиент 2: обычный закрытый заказ; клиент остаётся новичком ---
    _order(2, "Закрыт", "03/11/2026", "03/11/2026", 40_000,
           "Диагностика и свечи",
           [(8, "Диагностика подвески", 1500.0)],
           [("Свечи зажигания", 900.0, 4, 5, "шт", 600.0)])

    # --- Открытые заказы: в зачёт лояльности не идут ---
    _order(11, "Новый", "10/11/2026", "", 95_000,
           "Заказан чип-тюнинг (в работе не начат)",
           [(4, "Чип-тюнинг Stage 1", 45_000.0)])

    _order(12, "В работе", "12/11/2026", "", 12_000,
           "Комплексное ТО, ожидает запчасти",
           [(2, "Комплексное ТО", 8500.0)],
           [("Масло моторное 5W-40", 800.0, 4, 1, "л", 600.0)])


# ==================== РЕНДЕРИНГ SQL ====================

def sql_literal(value) -> str:
    """Привести значение Python к SQL-литералу."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return repr(value)
    text = str(value).replace("'", "''")
    return f"'{text}'"


def order_id(index: int, created: str) -> str:
    """ID в формате приложения: ZAK-ДД/ММ/ГГ-NNNN."""
    day, month, year = created.split("/")
    return f"ZAK-{day}/{month}/{year[2:]}-{index:04d}"


def order_total(order) -> float:
    total = sum(price for _, _, price in order["services"])
    total += sum(price * qty for _, price, qty, *_ in order["parts"])
    return round(total, 2)


SERVICE_CATEGORY = {sid: category for sid, _, _, _, _, category in SERVICES}
PART_CATEGORY = {pid: category for pid, _, _, _, _, _, _, _, _, category, _ in SPARE_PARTS}


def is_maintenance(order) -> bool:
    return any(SERVICE_CATEGORY.get(sid) == "MAINTENANCE" for sid, _, _ in order["services"])


def computed_tuning(client_id: int) -> float:
    """Сумма тюнинга так, как её считает LoyaltyService.calculateTuningAmount()."""
    amount = 0.0
    for order in ORDERS:
        if order["client"] != client_id or order["status"] != "Закрыт":
            continue
        for sid, _, price in order["services"]:
            if SERVICE_CATEGORY.get(sid) == "TUNING":
                amount += price
        for _, price, qty, part_id, *_ in order["parts"]:
            if PART_CATEGORY.get(part_id) == "TUNING":
                amount += price * qty
    return round(amount, 2)


def computed_stars(client_id: int) -> int:
    """Звёзды ТО так, как их начисляет LoyaltyService (шаг 5 000–10 000 км, максимум 5)."""
    mileages = sorted(order["mileage"] for order in ORDERS
                      if order["client"] == client_id
                      and order["status"] == "Закрыт"
                      and is_maintenance(order)
                      and order["mileage"])
    stars = 0
    previous = None
    for mileage in mileages:
        if previous is None or 5_000 <= mileage - previous <= 10_000:
            stars = min(5, stars + 1)
        previous = mileage
    return stars


def verify_consistency() -> None:
    """Проверить, что клиенты согласованы с заказами (иначе статусы будут ложными)."""
    problems = []
    for client_id, (_, last_name, _, stars, tuning, _) in CLIENTS.items():
        expected_stars = computed_stars(client_id)
        if expected_stars != stars:
            problems.append(f"клиент {client_id}: звёзд в данных {stars}, по заказам {expected_stars}")
        expected_tuning = computed_tuning(client_id)
        if abs(expected_tuning - tuning) > 0.01:
            problems.append(f"клиент {client_id}: тюнинг в данных {tuning:.0f}, по заказам {expected_tuning:.0f}")
    if problems:
        raise SystemExit("Данные не согласованы:\n  " + "\n  ".join(problems))


def render_sql(force_delete: bool) -> str:
    lines = [
        "-- AdminSTO: тестовые данные для демонстрационной базы.",
        "-- Сгенерировано tools/seed_test_data.py",
        f"-- Дата генерации: {dt.datetime.now():%d.%m.%Y %H:%M}",
        "",
        "BEGIN TRANSACTION;",
        "",
    ]

    if force_delete:
        client_ids = ", ".join(str(cid) for cid in CLIENTS)
        lines += [
            "-- Перезалив: убираем ранее созданные строки",
            f"DELETE FROM order_parts WHERE order_id IN (SELECT id FROM orders WHERE client_id IN ({client_ids}));",
            f"DELETE FROM order_services WHERE order_id IN (SELECT id FROM orders WHERE client_id IN ({client_ids}));",
            f"DELETE FROM orders WHERE client_id IN ({client_ids});",
            f"DELETE FROM client_cars WHERE client_id IN ({client_ids});",
            f"DELETE FROM clients WHERE id IN ({client_ids});",
            "DELETE FROM services WHERE id BETWEEN 1 AND 9;",
            "DELETE FROM spare_parts WHERE id BETWEEN 1 AND 7;",
            "",
        ]

    lines.append("-- Справочник услуг")
    for sid, name, price, duration, part_number, category in SERVICES:
        lines.append(
            "INSERT INTO services (id, name, price, duration, part_number, category) VALUES ("
            f"{sid}, {sql_literal(name)}, {sql_literal(price)}, {duration}, "
            f"{sql_literal(part_number)}, {sql_literal(category)});"
        )

    lines += ["", "-- Справочник запчастей"]
    for (pid, name, part_number, manufacturer, retail, purchase,
         stock, min_stock, location, category, unit_type) in SPARE_PARTS:
        lines.append(
            "INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, "
            "purchase_price, stock, min_stock, location, category, unit_type) VALUES ("
            f"{pid}, {sql_literal(name)}, {sql_literal(part_number)}, {sql_literal(manufacturer)}, "
            f"{sql_literal(retail)}, {sql_literal(purchase)}, {sql_literal(stock)}, "
            f"{sql_literal(min_stock)}, {sql_literal(location)}, {sql_literal(category)}, "
            f"{sql_literal(unit_type)});"
        )

    lines += ["", "-- Клиенты и их автомобили"]
    last_repair = {}
    for order in ORDERS:
        if order["status"] == "Закрыт" and order["closed"]:
            day, month, year = order["closed"].split("/")
            key = dt.date(int(year), int(month), int(day))
            client_id = order["client"]
            if client_id not in last_repair or key > last_repair[client_id]:
                last_repair[client_id] = key

    for cid, (name, last_name, phone, stars, tuning, cars) in CLIENTS.items():
        car_model, car_number, _ = cars[0]
        repair_date = last_repair[cid].strftime("%d/%m/%Y") if cid in last_repair else ""
        lines.append(
            "INSERT INTO clients (id, name, last_name, phone, car_model, car_number, "
            "last_repair_date, maintenance_stars, tuning_total) VALUES ("
            f"{cid}, {sql_literal(name)}, {sql_literal(last_name)}, {sql_literal(phone)}, "
            f"{sql_literal(car_model)}, {sql_literal(car_number)}, {sql_literal(repair_date)}, "
            f"{stars}, {sql_literal(tuning)});"
        )

    lines.append("")
    for cid, (_, _, _, _, _, cars) in CLIENTS.items():
        for car_model, car_number, mileage in cars:
            lines.append(
                "INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES ("
                f"{cid}, {sql_literal(car_model)}, {sql_literal(car_number)}, {mileage});"
            )

    lines += ["", "-- Заказы, их услуги и запчасти"]
    total_services = 0
    total_parts = 0
    for index, order in enumerate(ORDERS, start=1):
        oid = order_id(index, order["created"])
        lines.append(
            "INSERT INTO orders (id, client_id, status, total, created_date, mileage, "
            "closed_date, notes) VALUES ("
            f"{sql_literal(oid)}, {order['client']}, {sql_literal(order['status'])}, "
            f"{sql_literal(order_total(order))}, {sql_literal(order['created'])}, "
            f"{order['mileage']}, {sql_literal(order['closed'])}, "
            f"{sql_literal(order['notes'])});"
        )
        for service_id, service_name, price in order["services"]:
            lines.append(
                "INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ("
                f"{sql_literal(oid)}, {sql_literal(service_name)}, {sql_literal(price)}, {service_id});"
            )
            total_services += 1
        for part_name, price, qty, spare_part_id, unit_type, purchase in order["parts"]:
            lines.append(
                "INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, "
                "unit_type, purchase_price) VALUES ("
                f"{sql_literal(oid)}, {sql_literal(part_name)}, {sql_literal(price)}, {qty}, "
                f"{spare_part_id}, {sql_literal(unit_type)}, {sql_literal(purchase)});"
            )
            total_parts += 1

    lines += [
        "",
        "COMMIT;",
        "",
        f"-- Итого: клиентов {len(CLIENTS)}, услуг {len(SERVICES)}, запчастей {len(SPARE_PARTS)}, "
        f"заказов {len(ORDERS)}, строк услуг в заказах {total_services}, строк запчастей {total_parts}",
        "",
    ]
    return "\n".join(lines)


# ==================== ПРОВЕРКА ====================

def loyalty_stage(tuning_total: float) -> str:
    """Повторить правило LoyaltyService.getTuningStage()."""
    if tuning_total > 300_000:
        return "Stage 3"
    if tuning_total > 100_000:
        return "Stage 2"
    if tuning_total >= 30_000:
        return "Stage 1"
    return "без stage"


def report(db_path: Path) -> None:
    con = sqlite3.connect(db_path)
    con.row_factory = sqlite3.Row
    cur = con.cursor()

    print("\n=== Строк в таблицах ===")
    for table in ("clients", "client_cars", "services", "spare_parts", "orders",
                  "order_services", "order_parts", "appointments"):
        count = cur.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        print(f"  {table:18} {count}")

    print("\n=== Клиенты и статусы лояльности ===")
    rows = cur.execute(
        "SELECT id, name, last_name, phone, car_model, maintenance_stars, tuning_total "
        "FROM clients ORDER BY id"
    ).fetchall()
    for row in rows:
        stars = "★" * row["maintenance_stars"] + "☆" * (5 - row["maintenance_stars"])
        print(f"  #{row['id']:<3} {row['name']} {row['last_name']:<11} {row['phone']:<18} "
              f"{row['car_model']:<20} {stars}  tuning={row['tuning_total']:>10,.0f} "
              f"({loyalty_stage(row['tuning_total'])})")

    print("\n=== Заказы ===")
    rows = cur.execute(
        "SELECT o.id, o.client_id, o.status, o.total, o.created_date, o.mileage, "
        "  (SELECT GROUP_CONCAT(service_name, ', ') FROM order_services WHERE order_id = o.id) AS services, "
        "  (SELECT COUNT(*) FROM order_parts WHERE order_id = o.id) AS parts "
        "FROM orders o ORDER BY o.created_date, o.id"
    ).fetchall()
    for row in rows:
        parts_note = ""
        if row["parts"]:
            parts_note = f" (+{row['parts']} поз. запчастей)"
        print(f"  {row['id']:<22} client={row['client_id']:<3} {row['status']:<8} "
              f"{row['created_date']:<11} {row['total']:>10,.0f} руб.  "
              f"пробег {row['mileage']:>7,}  {row['services'] or '-'}{parts_note}")

    print("\n=== Сводка ===")
    closed = cur.execute("SELECT COUNT(*) FROM orders WHERE status = 'Закрыт'").fetchone()[0]
    opened = cur.execute("SELECT COUNT(*) FROM orders WHERE status <> 'Закрыт'").fetchone()[0]
    print(f"  закрытых заказов: {closed}, открытых: {opened}")
    print(f"  проверка ФК: {'ошибок нет' if not cur.execute('PRAGMA foreign_key_check').fetchall() else 'ЕСТЬ ОШИБКИ'}")
    print(f"  целостность: {cur.execute('PRAGMA quick_check').fetchone()[0]}")
    con.close()


# ==================== MAIN ====================

def main() -> int:
    parser = argparse.ArgumentParser(description="Наполнить базу AdminSTO тестовыми данными")
    parser.add_argument("--db", required=True, help="путь к autoservice.db")
    parser.add_argument("--apply", action="store_true", help="выполнить SQL в базе (по умолчанию только печать)")
    parser.add_argument("--force", action="store_true", help="удалить ранее созданные тестовые строки перед вставкой")
    parser.add_argument("--sql-out", default=str(Path(__file__).with_name("seed_test_data.sql")),
                        help="куда сохранить сгенерированный SQL")
    parser.add_argument("--backup", action="store_true",
                        help="сделать копию базы перед изменением (включается автоматически с --apply)")
    args = parser.parse_args()

    db_path = Path(args.db)
    if not db_path.exists():
        print(f"База не найдена: {db_path}", file=sys.stderr)
        return 2

    build_orders()
    verify_consistency()
    script = render_sql(force_delete=args.force)

    sql_out = Path(args.sql_out)
    sql_out.write_text(script, encoding="utf-8")
    print(f"SQL сохранён: {sql_out}")

    if not args.apply:
        print("Режим без --apply: база не изменялась.")
        return 0

    if args.backup or args.apply:
        stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
        backup_dir = Path(__file__).resolve().parent.parent / ".tmp" / "seeds" / f"backup-{stamp}"
        backup_dir.mkdir(parents=True, exist_ok=True)
        backup_path = backup_dir / db_path.name
        shutil.copy2(db_path, backup_path)
        print(f"Резервная копия: {backup_path}")

    con = sqlite3.connect(db_path)
    try:
        with con:
            con.executescript(script)
    except sqlite3.Error as exc:
        print(f"Ошибка выполнения: {exc}", file=sys.stderr)
        return 1
    finally:
        con.close()

    report(db_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
