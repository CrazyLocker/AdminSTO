with open('docs/closed_orders_2026.sql', 'r', encoding='utf-8') as f:
    content = f.read()

orders = content.count('INSERT INTO "orders"')
osvc = content.count('INSERT INTO "order_services"')
oparts = content.count('INSERT INTO "order_parts"')

print(f'Orders: {orders}')
print(f'Order services: {osvc}')
print(f'Order parts: {oparts}')
print(f'Has BEGIN TRANSACTION: {"BEGIN TRANSACTION" in content}')
print(f'Has COMMIT: {"COMMIT" in content}')
print(f'Has DELETE: {"DELETE FROM" in content}')
print(f'All closed (Закрыт count == orders): {content.count("Закрыт") == orders}')

# Check unique order IDs
import re
order_ids = re.findall(r"'(ZAK-[0-9/]+-[0-9]+)'", content)
# Get unique order IDs from orders table only
order_id_lines = [l for l in content.split('\n') if 'INSERT INTO "orders"' in l]
unique_ids = set()
for line in order_id_lines:
    m = re.search(r"'(ZAK-[0-9/]+-[0-9]+)'", line)
    if m:
        unique_ids.add(m.group(1))
print(f'Unique order IDs: {len(unique_ids)}')

# Check date distribution
months = {1:0, 2:0, 3:0, 4:0, 5:0, 6:0, 7:0, 8:0}
for uid in unique_ids:
    parts = uid.split('-')
    date_part = parts[1]  # DD/MM/26
    month = int(date_part.split('/')[1])
    months[month] += 1

print(f'Distribution by month: {months}')

# Check all services are from existing list
existing_services = [
    "Замена масла ДВС", "Диагностика ходовой", "Дезинфекция системы кондиционирования",
    "Замена тормозных колодок (перед)", "Диагностика тормозной системы",
    "Диагностика электрооборудования", "Замена аккумулятора", "Замена амортизаторов задних",
    "Замена амортизаторов передних", "Замена воздушного фильтра", "Замена свечей зажигания",
    "Замена гидрокомпенсаторов", "Замена главного тормозного цилиндра",
    "Замена задних тормозных дисков", "Замена задних тормозных колодок",
    "Замена компрессора кондиционера", "Замена масла в АКПП", "Замена масла в ДВС",
    "Замена масла в МКПП", "Замена масла в раздаточной коробке", "Замена масла в редукторе",
    "Замена маслоотделителя", "Замена опор двигателя", "Замена охлаждающей жидкости",
    "Замена охлаждающей жидкости (антифриз)", "Замена помпы двигателя",
    "Замена передних тормозных дисков", "Замена приводных ремней",
    "Замена прокладки клапанной крышки", "Замена прокладки поддона",
    "Замена пружин подвески", "Замена радиатора охлаждения", "Замена рулевых тяг",
    "Замена сальников коленвала", "Замена рулевых наконечников", "Замена сайлентблоков",
    "Замена ступичного подшипника", "Замена стартера", "Замена суппорта",
    "Замена топливного фильтра", "Замена тормозных колодок (зад)",
    "Замена турбины", "Замена тормозных шлангов", "Замена фильтра салона",
    "Замена цепи ГРМ", "Замена шаровых опор", "Замена щеток стеклоочистителя",
    "Замер компрессии", "Заправка кондиционера", "Кап. ремонт двигателя",
    "Комплексная диагностика двигателя", "Компьютерная диагностика", "Мойка двигателя",
    "Проверка кузова", "Прокачка тормозной системы", "Развал-схождение",
    "Регулировка ручного тормоза", "Устранение течи масла", "Химчистка салона",
    "Шиномонтаж (1 колесо)", "Эндоскопия цилиндров",
]

for line in content.split('\n'):
    if 'INSERT INTO "order_services"' in line:
        import re
        m = re.search(r"'([^']+)',(\d+\.\d+),(\d+)", line)
        if m:
            svc_name = m.group(1)
            if svc_name not in existing_services:
                print(f'UNKNOWN SERVICE: {svc_name}')

print('Validation complete!')
