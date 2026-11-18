#!/usr/bin/env python3
"""Наполняет таблицу appointments демонстрационными записями.

Скрипт рассчитан на уже существующую базу (клиенты и услуги должны быть),
поэтому его удобно запускать на портативной сборке заказчика, чтобы на
альфа-тесте календарь «Записи» не выглядел пустым.

По умолчанию записи создаются на текущую и следующую недели. Если в базе
уже есть записи, скрипт откажется работать: нужно явно указать --replace,
чтобы существующие записи были заменены демонстрационными.

Примеры:
    python tools/seed_appointments.py --db T:/AdminSTO/autoservice.db --dry-run
    python tools/seed_appointments.py --db T:/AdminSTO/autoservice.db --replace
"""

from __future__ import annotations

import argparse
import random
import sqlite3
import sys
from dataclasses import dataclass
from datetime import date, datetime, timedelta

DB_DATE_FORMAT = "%d/%m/%Y"

TIME_SLOTS = [
    "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
    "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00",
]

MASTERS = ["Саныч", "Малой"]

# Appointment.STATUS_* из доменной модели
STATUS_NEW = "Новая"
STATUS_CONFIRMED = "Подтверждена"
STATUS_CANCELLED = "Отменена"
STATUS_COMPLETED = "Выполнено"

STATUSES = [STATUS_NEW, STATUS_CONFIRMED, STATUS_CONFIRMED, STATUS_COMPLETED]


@dataclass(frozen=True)
class ClientInfo:
    client_id: int
    title: str


@dataclass(frozen=True)
class ServiceInfo:
    service_id: int
    name: str


@dataclass(frozen=True)
class PlannedAppointment:
    client_id: int
    order_id: str | None
    master: str
    service_id: int
    service_name: str
    day: date
    time: str
    status: str


def monday_of(day: date) -> date:
    return day - timedelta(days=day.weekday())


def load_clients(con: sqlite3.Connection) -> list[ClientInfo]:
    rows = con.execute(
        "SELECT id, last_name, name FROM clients ORDER BY id"
    ).fetchall()
    clients: list[ClientInfo] = []
    for client_id, last_name, name in rows:
        title = " ".join(part for part in ((last_name or "").strip(), (name or "").strip()) if part)
        clients.append(ClientInfo(client_id=client_id, title=title or f"Клиент #{client_id}"))
    return clients


def load_services(con: sqlite3.Connection) -> list[ServiceInfo]:
    rows = con.execute("SELECT id, name FROM services ORDER BY id").fetchall()
    return [ServiceInfo(service_id=service_id, name=name) for service_id, name in rows]


def load_closed_orders(con: sqlite3.Connection) -> dict[int, str]:
    """Возвращает id клиента -> id последнего закрытого заказа."""
    rows = con.execute(
        """
        SELECT client_id, id
          FROM orders
         WHERE status = 'Закрыт' AND client_id IS NOT NULL
         ORDER BY created_date
        """
    ).fetchall()
    latest: dict[int, str] = {}
    for client_id, order_id in rows:
        latest[int(client_id)] = order_id
    return latest


def build_plan(
    clients: list[ClientInfo],
    services: list[ServiceInfo],
    closed_orders: dict[int, str],
    today: date,
    weeks: int,
    per_week: int,
    seed: int,
) -> list[PlannedAppointment]:
    rng = random.Random(seed)
    plan: list[PlannedAppointment] = []
    start = monday_of(today)
    used_slots: set[tuple[date, str, str]] = set()

    for week in range(weeks):
        week_start = start + timedelta(weeks=week)
        days = [week_start + timedelta(days=offset) for offset in range(6)]  # Пн..Сб
        created = 0
        attempts = 0
        while created < per_week and attempts < per_week * 40:
            attempts += 1
            day = rng.choice(days)
            slot = rng.choice(TIME_SLOTS)
            master = rng.choice(MASTERS)
            key = (day, slot, master)
            if key in used_slots:
                continue

            client = rng.choice(clients)
            service = rng.choice(services)
            status = rng.choice(STATUSES)
            order_id = closed_orders.get(client.client_id) if rng.random() < 0.45 else None

            used_slots.add(key)
            plan.append(
                PlannedAppointment(
                    client_id=client.client_id,
                    order_id=order_id,
                    master=master,
                    service_id=service.service_id,
                    service_name=service.name,
                    day=day,
                    time=slot,
                    status=status,
                )
            )
            created += 1

    plan.sort(key=lambda item: (item.day, item.time))
    return plan


def print_plan(plan: list[PlannedAppointment], titles: dict[int, str]) -> None:
    print(f"{'Дата':<12} {'Время':<6} {'Мастер':<8} {'Клиент':<28} {'Статус':<14} Услуга")
    for item in plan:
        print(
            f"{item.day.strftime('%d.%m.%Y'):<12} {item.time:<6} {item.master:<8} "
            f"{titles.get(item.client_id, '—'):<28} {item.status:<14} {item.service_name}"
        )


def apply_plan(con: sqlite3.Connection, plan: list[PlannedAppointment], replace: bool) -> int:
    if replace:
        con.execute("DELETE FROM appointments")

    before = con.execute("SELECT COUNT(*) FROM appointments").fetchone()[0]
    con.executemany(
        """
        INSERT INTO appointments
            (client_id, order_id, master_name, service_name, service_id,
             appointment_date, appointment_time, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        [
            (
                item.client_id,
                item.order_id,
                item.master,
                item.service_name,
                item.service_id,
                item.day.strftime(DB_DATE_FORMAT),
                item.time,
                item.status,
            )
            for item in plan
        ],
    )
    con.commit()
    after = con.execute("SELECT COUNT(*) FROM appointments").fetchone()[0]
    return after - before


def verify(con: sqlite3.Connection) -> None:
    foreign_key_errors = con.execute("PRAGMA foreign_key_check").fetchall()
    if foreign_key_errors:
        raise SystemExit(f"Нарушены внешние ключи: {foreign_key_errors}")

    print("\nПроверка:")
    print(f"  quick_check          : {con.execute('PRAGMA quick_check').fetchone()[0]}")
    print(f"  записей всего        : {con.execute('SELECT COUNT(*) FROM appointments').fetchone()[0]}")
    print("  по статусам:")
    for status, count in con.execute(
        "SELECT status, COUNT(*) FROM appointments GROUP BY status ORDER BY status"
    ):
        print(f"    {status:<14} {count}")
    print("  по датам:")
    for appointment_date, count in con.execute(
        """
        SELECT appointment_date, COUNT(*)
          FROM appointments
         GROUP BY appointment_date
         ORDER BY substr(appointment_date, 7, 4) || substr(appointment_date, 4, 2) ||
                  substr(appointment_date, 1, 2)
        """
    ):
        print(f"    {appointment_date:<12} {count}")

    dangling = con.execute(
        """
        SELECT COUNT(*)
          FROM appointments a
          LEFT JOIN clients c ON c.id = a.client_id
         WHERE c.id IS NULL
        """
    ).fetchone()[0]
    if dangling:
        raise SystemExit(f"Записей со ссылкой на несуществующего клиента: {dangling}")


def main(argv: list[str]) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Демонстрационные записи для таблицы appointments")
    parser.add_argument("--db", required=True, help="путь к файлу базы данных")
    parser.add_argument("--replace", action="store_true", help="удалить существующие записи перед вставкой")
    parser.add_argument("--dry-run", action="store_true", help="только показать план, база не меняется")
    parser.add_argument("--weeks", type=int, default=2, help="на сколько недель формировать записи (по умолчанию 2)")
    parser.add_argument("--per-week", type=int, default=9, help="записей на неделю (по умолчанию 9)")
    parser.add_argument("--seed", type=int, default=20261115, help="зерно генератора (для повторяемости)")
    args = parser.parse_args(argv)

    con = sqlite3.connect(args.db)
    con.execute("PRAGMA foreign_keys = ON")

    clients = load_clients(con)
    services = load_services(con)
    if not clients:
        raise SystemExit("В базе нет клиентов — сначала нужны клиенты и услуги")
    if not services:
        raise SystemExit("В базе нет услуг — сначала нужны клиенты и услуги")

    existing = con.execute("SELECT COUNT(*) FROM appointments").fetchone()[0]
    if existing and not args.replace and not args.dry_run:
        raise SystemExit(
            f"В таблице appointments уже {existing} записей. "
            "Запустите с --replace, чтобы заменить их демонстрационными."
        )

    closed_orders = load_closed_orders(con)
    plan = build_plan(
        clients=clients,
        services=services,
        closed_orders=closed_orders,
        today=date.today(),
        weeks=max(1, args.weeks),
        per_week=max(1, args.per_week),
        seed=args.seed,
    )

    titles = {client.client_id: client.title for client in clients}
    print_plan(plan, titles)

    if args.dry_run:
        print(f"\nРежим --dry-run: план из {len(plan)} записей, база не изменялась.")
        return 0

    added = apply_plan(con, plan, replace=args.replace)
    print(f"\nДобавлено записей: {added}")
    verify(con)
    print(f"\nГотово: {args.db} — {datetime.now():%d.%m.%Y %H:%M:%S}")
    con.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
