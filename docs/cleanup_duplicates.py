import sqlite3
import os

def cleanup_duplicate_service_spare_parts(db_path='autoservice.db'):
    """
    Удаляет дубликаты связей между услугами и запчастями.
    
    Оставляет только одну связь для каждой уникальной пары
    (service_id, spare_part_id), удаляя остальные дубликаты.
    
    :param db_path: Путь к файлу базы данных SQLite
    """
    
    if not os.path.exists(db_path):
        print(f"Ошибка: Файл базы данных не найден: {db_path}")
        return False
    
    print(f"Подключение к базе данных: {db_path}")
    
    conn = sqlite3.connect(db_path)
    conn.execute("PRAGMA foreign_keys = ON")
    
    cursor = conn.cursor()
    
    try:
        # Сначала получаем информацию о текущем состоянии
        print("\n=== Текущее состояние ===")
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts")
        total_before = cursor.fetchone()[0]
        print(f"Всего связей: {total_before}")
        
        # Группируем по service_id и spare_part_id, считаем дубликаты
        cursor.execute("""
            SELECT service_id, spare_part_id, COUNT(*) as count
            FROM service_spare_parts
            GROUP BY service_id, spare_part_id
            HAVING COUNT(*) > 1
        """)
        duplicate_groups = cursor.fetchall()
        print(f"Групп с дубликатами: {len(duplicate_groups)}")
        
        for group in duplicate_groups:
            service_id, spare_part_id, count = group
            print(f"  service_id={service_id}, spare_part_id={spare_part_id}: {count} дубликатов")
        
        # Удаляем дубликаты, оставляя только одну запись с минимальным id
        print("\n=== Удаление дубликатов ===")
        
        cursor.execute("""
            DELETE FROM service_spare_parts 
            WHERE id NOT IN (
                SELECT MIN(id) 
                FROM service_spare_parts 
                GROUP BY service_id, spare_part_id
            )
        """)
        deleted_count = cursor.rowcount
        print(f"Удалено дубликатов: {deleted_count}")
        
        # Подтверждаем изменения
        conn.commit()
        
        # Проверяем результат
        print("\n=== Проверка результата ===")
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts")
        total_after = cursor.fetchone()[0]
        print(f"Осталось связей: {total_after}")
        
        # Проверяем, остались ли дубликаты
        cursor.execute("""
            SELECT COUNT(*) FROM (
                SELECT service_id, spare_part_id, COUNT(*) as count
                FROM service_spare_parts
                GROUP BY service_id, spare_part_id
                HAVING COUNT(*) > 1
            )
        """)
        remaining_duplicates = cursor.fetchone()[0]
        
        if remaining_duplicates == 0:
            print("Дубликатов не осталось!")
        else:
            print(f"Осталось {remaining_duplicates} групп с дубликатами")
        
        print(f"\nДо: {total_before} связей")
        print(f"После: {total_after} связей")
        print(f"Удалено: {deleted_count} дубликатов")
        
        if remaining_duplicates == 0:
            print("\n[OK] Дубликаты успешно удалены!")
            return True
        else:
            print("\n[FAIL] Не все дубликаты были удалены!")
            return False
            
    except Exception as e:
        print(f"\nОшибка при удалении дубликатов: {e}")
        conn.rollback()
        return False
    finally:
        conn.close()
        print(f"\nБаза данных закрыта.")

if __name__ == "__main__":
    import sys
    
    # Используем базовый путь к базе данных
    db_path = 'autoservice.db'
    
    # Если указан аргумент, используем его как путь к БД
    if len(sys.argv) > 1:
        db_path = sys.argv[1]
    
    print("=" * 60)
    print("Скрипт удаления дубликатов связей услуга-запчасть")
    print("=" * 60)
    
    success = cleanup_duplicate_service_spare_parts(db_path)
    
    if success:
        print("\nСкрипт выполнен успешно!")
        sys.exit(0)
    else:
        print("\nСкрипт завершился с ошибками!")
        sys.exit(1)
