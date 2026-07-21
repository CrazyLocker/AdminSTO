import sqlite3
import os

def cleanup_service_spare_parts(db_path='autoservice.db'):
    """
    Очищает все связи между услугами и запчастями в базе данных.
    
    Удаляет данные из следующих таблиц:
    - service_spare_parts (старая структура)
    - service_spare_parts_lists (новая структура)
    - service_spare_parts_list_items (элементы списков)
    
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
        count_spare_parts = cursor.fetchone()[0]
        print(f"Связей в service_spare_parts: {count_spare_parts}")
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts_lists")
        count_lists = cursor.fetchone()[0]
        print(f"Списков в service_spare_parts_lists: {count_lists}")
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts_list_items")
        count_items = cursor.fetchone()[0]
        print(f"Элементов в service_spare_parts_list_items: {count_items}")
        
        # Удаляем данные из service_spare_parts_list_items (сначала, т.к. есть FK)
        print("\n=== Удаление данных ===")
        
        cursor.execute("DELETE FROM service_spare_parts_list_items")
        deleted_items = cursor.rowcount
        print(f"Удалено элементов из service_spare_parts_list_items: {deleted_items}")
        
        # Удаляем данные из service_spare_parts_lists
        cursor.execute("DELETE FROM service_spare_parts_lists")
        deleted_lists = cursor.rowcount
        print(f"Удалено списков из service_spare_parts_lists: {deleted_lists}")
        
        # Удаляем данные из service_spare_parts (старая структура)
        cursor.execute("DELETE FROM service_spare_parts")
        deleted_spare_parts = cursor.rowcount
        print(f"Удалено связей из service_spare_parts: {deleted_spare_parts}")
        
        # Подтверждаем изменения
        conn.commit()
        
        # Проверяем, что всё удалено
        print("\n=== Проверка результата ===")
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts")
        final_spare_parts = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts_lists")
        final_lists = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM service_spare_parts_list_items")
        final_items = cursor.fetchone()[0]
        
        print(f"service_spare_parts: {final_spare_parts}")
        print(f"service_spare_parts_lists: {final_lists}")
        print(f"service_spare_parts_list_items: {final_items}")
        
        if final_spare_parts == 0 and final_lists == 0 and final_items == 0:
            print("\n[OK] Все связи успешно удалены!")
            return True
        else:
            print("\n[FAIL] Не удалось удалить все связи!")
            return False
            
    except Exception as e:
        print(f"\nОшибка при удалении: {e}")
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
    print("Скрипт очистки связей услуга-запчасть")
    print("=" * 60)
    
    success = cleanup_service_spare_parts(db_path)
    
    if success:
        print("\nСкрипт выполнен успешно!")
        sys.exit(0)
    else:
        print("\nСкрипт завершился с ошибками!")
        sys.exit(1)
