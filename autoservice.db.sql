BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS "app_settings" (
	"id"	INTEGER,
	"setting_key"	TEXT NOT NULL CHECK(length("setting_key") > 0) UNIQUE,
	"setting_value"	TEXT NOT NULL CHECK(length("setting_value") > 0),
	"description"	TEXT DEFAULT '',
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "appointments" (
	"id"	INTEGER,
	"client_id"	INTEGER NOT NULL CHECK("client_id" > 0),
	"order_id"	TEXT,
	"master_name"	TEXT NOT NULL CHECK(length("master_name") > 0),
	"service_name"	TEXT NOT NULL CHECK(length("service_name") > 0),
	"service_id"	INTEGER DEFAULT 0,
	"appointment_date"	TEXT NOT NULL CHECK(length("appointment_date") > 0),
	"appointment_time"	TEXT NOT NULL CHECK(length("appointment_time") > 0),
	"status"	TEXT NOT NULL CHECK(length("status") > 0),
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("client_id") REFERENCES "clients"("id"),
	FOREIGN KEY("order_id") REFERENCES "orders"("id")
);
CREATE TABLE IF NOT EXISTS "client_cars" (
	"id"	INTEGER,
	"client_id"	INTEGER NOT NULL CHECK("client_id" > 0),
	"car_model"	TEXT NOT NULL CHECK(length("car_model") > 0),
	"car_number"	TEXT NOT NULL CHECK(length("car_number") > 0),
	"mileage"	INTEGER DEFAULT 0 CHECK("mileage" >= 0),
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("client_id") REFERENCES "clients"("id") ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS "clients" (
	"id"	INTEGER,
	"name"	TEXT NOT NULL CHECK(length("name") > 0),
	"last_name"	TEXT DEFAULT '',
	"phone"	TEXT NOT NULL CHECK(length("phone") > 0),
	"car_model"	TEXT DEFAULT '',
	"car_number"	TEXT DEFAULT '',
	"last_repair_date"	TEXT DEFAULT '',
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "order_parts" (
	"order_id"	TEXT NOT NULL CHECK(length("order_id") > 0),
	"part_name"	TEXT NOT NULL CHECK(length("part_name") > 0),
	"price"	REAL NOT NULL CHECK("price" >= 0),
	"quantity"	INTEGER NOT NULL CHECK("quantity" > 0),
	"spare_part_id"	INTEGER DEFAULT 0,
	"unit_type"	TEXT DEFAULT 'шт',
	"purchase_price"	REAL DEFAULT 0,
	FOREIGN KEY("order_id") REFERENCES "orders"("id") ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS "order_services" (
	"order_id"	TEXT NOT NULL CHECK(length("order_id") > 0),
	"service_name"	TEXT NOT NULL CHECK(length("service_name") > 0),
	"price"	REAL NOT NULL CHECK("price" >= 0),
	"service_id"	INTEGER DEFAULT 0,
	FOREIGN KEY("order_id") REFERENCES "orders"("id") ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS "orders" (
	"id"	TEXT CHECK(length("id") > 0),
	"client_id"	INTEGER NOT NULL CHECK("client_id" > 0),
	"status"	TEXT NOT NULL CHECK(length("status") > 0),
	"total"	REAL NOT NULL CHECK("total" >= 0),
	"created_date"	TEXT NOT NULL CHECK(length("created_date") > 0),
	"mileage"	INTEGER DEFAULT 0 CHECK("mileage" >= 0),
	"closed_date"	TEXT DEFAULT '',
	"notes"	TEXT DEFAULT '',
	"car_model"	TEXT DEFAULT '',
	"car_number"	TEXT DEFAULT '',
	PRIMARY KEY("id"),
	FOREIGN KEY("client_id") REFERENCES "clients"("id")
);
CREATE TABLE IF NOT EXISTS "service_parts" (
	"id"	INTEGER,
	"service_id"	INTEGER NOT NULL CHECK("service_id" > 0),
	"spare_part_id"	INTEGER NOT NULL CHECK("spare_part_id" > 0),
	"quantity"	REAL DEFAULT 1 CHECK("quantity" > 0),
	"is_required"	INTEGER DEFAULT 1 CHECK("is_required" IN (0, 1)),
	"created_date"	TEXT NOT NULL CHECK(length("created_date") > 0),
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("service_id") REFERENCES "services"("id"),
	FOREIGN KEY("spare_part_id") REFERENCES "spare_parts"("id")
);
CREATE TABLE IF NOT EXISTS "service_spare_parts" (
	"id"	INTEGER,
	"service_id"	INTEGER NOT NULL CHECK("service_id" > 0),
	"spare_part_id"	INTEGER NOT NULL CHECK("spare_part_id" > 0),
	"quantity"	INTEGER DEFAULT 1 CHECK("quantity" > 0),
	"unit_type"	TEXT DEFAULT 'шт' CHECK("unit_type" IN ('шт', 'л', 'компл')),
	"active"	INTEGER DEFAULT 1 CHECK("active" IN (0, 1)),
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("service_id") REFERENCES "services"("id"),
	FOREIGN KEY("spare_part_id") REFERENCES "spare_parts"("id")
);
CREATE TABLE IF NOT EXISTS "service_spare_parts_list_items" (
	"id"	INTEGER,
	"list_id"	INTEGER NOT NULL CHECK("list_id" > 0),
	"spare_part_id"	INTEGER NOT NULL CHECK("spare_part_id" > 0),
	"quantity"	INTEGER DEFAULT 1 CHECK("quantity" > 0),
	"unit_type"	TEXT DEFAULT 'шт' CHECK("unit_type" IN ('шт', 'л', 'компл')),
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("list_id") REFERENCES "service_spare_parts_lists"("id") ON DELETE CASCADE,
	FOREIGN KEY("spare_part_id") REFERENCES "spare_parts"("id")
);
CREATE TABLE IF NOT EXISTS "service_spare_parts_lists" (
	"id"	INTEGER,
	"service_id"	INTEGER NOT NULL CHECK("service_id" > 0),
	"created_date"	TEXT NOT NULL CHECK(length("created_date") > 0),
	"active"	INTEGER DEFAULT 1 CHECK("active" IN (0, 1)),
	PRIMARY KEY("id" AUTOINCREMENT),
	FOREIGN KEY("service_id") REFERENCES "services"("id")
);
CREATE TABLE IF NOT EXISTS "services" (
	"id"	INTEGER,
	"name"	TEXT NOT NULL CHECK(length("name") > 0) UNIQUE,
	"price"	REAL NOT NULL CHECK("price" >= 0),
	"duration"	INTEGER DEFAULT 60 CHECK("duration" >= 0),
	"part_number"	TEXT DEFAULT '',
	"category_id"	INTEGER DEFAULT 0,
	"oil_volume"	REAL DEFAULT 0 CHECK("oil_volume" >= 0),
	"uses_oil"	INTEGER DEFAULT 0 CHECK("uses_oil" IN (0, 1)),
	"spare_part_name"	TEXT DEFAULT '',
	"spare_part_quantity"	INTEGER DEFAULT 0 CHECK("spare_part_quantity" >= 0),
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "spare_parts" (
	"id"	INTEGER,
	"name"	TEXT NOT NULL CHECK(length("name") > 0) UNIQUE,
	"part_number"	TEXT DEFAULT '',
	"manufacturer"	TEXT DEFAULT '',
	"compatible_models"	TEXT DEFAULT '',
	"note"	TEXT DEFAULT '',
	"purchase_price"	REAL CHECK("purchase_price" >= 0),
	"retail_price"	REAL NOT NULL CHECK("retail_price" >= 0),
	"stock"	REAL DEFAULT 0 CHECK("stock" >= 0),
	"min_stock"	REAL DEFAULT 0 CHECK("min_stock" >= 0),
	"location"	TEXT DEFAULT '',
	"unit_type"	TEXT DEFAULT 'шт' CHECK("unit_type" IN ('шт', 'л', 'компл')),
	PRIMARY KEY("id" AUTOINCREMENT)
);
CREATE TABLE IF NOT EXISTS "to_parts" (
	"id"	INTEGER,
	"car_model"	TEXT NOT NULL CHECK(length("car_model") > 0),
	"spare_part_id"	INTEGER NOT NULL CHECK("spare_part_id" > 0),
	"quantity"	INTEGER DEFAULT 1 CHECK("quantity" > 0),
	"unit_type"	TEXT DEFAULT 'шт' CHECK("unit_type" IN ('шт', 'л', 'компл')),
	"note"	TEXT DEFAULT '',
	"active"	INTEGER DEFAULT 1 CHECK("active" IN (0, 1)),
	PRIMARY KEY("id" AUTOINCREMENT)
);
INSERT INTO "clients" VALUES (1,'Петр','Петров','+79001234567','Haval Dargo','А123ВС163','2026-05-21');
INSERT INTO "clients" VALUES (2,'Елена','Смирнова','+79113456789','Haval Jolion','В456ЕК77','2026-05-22');
INSERT INTO "clients" VALUES (3,'Сергей','Козлов','+79224567890','Tank 300','М789НО50','2026-05-23');
INSERT INTO "clients" VALUES (4,'Анна','Новикова','+79335678901','Great Wall Poer','О159РТ40','2026-05-24');
INSERT INTO "clients" VALUES (5,'Дмитрий','Морозов','+79446789012','Haval F7','У357АЕ99','2026-05-25');
INSERT INTO "clients" VALUES (6,'Татьяна','Васильева','+79557890123','Haval Dargo','К753ВМ01','2026-05-26');
INSERT INTO "clients" VALUES (7,'Алексей','Николаев','+79668901234','Tank 500','Х951ТК22','2026-05-27');
INSERT INTO "clients" VALUES (8,'Мария','Фёдорова','+79779012345','Haval Jolion','Т357ХО18','2026-05-28');
INSERT INTO "clients" VALUES (9,'Владимир','Соколов','+79880123456','Great Wall Poer','А159АМ64','2026-05-29');
INSERT INTO "clients" VALUES (10,'Ольга','Михайлова','+79991234567','Haval F7','В753НС31','2026-05-30');
INSERT INTO "clients" VALUES (11,'Павел','Кузнецов','+79011234568','Haval Dargo','К123МР77','2026-05-31');
INSERT INTO "clients" VALUES (12,'Наталья','Попова','+79112345679','Tank 300','Х456РО99','2026-05-31');
INSERT INTO "clients" VALUES (13,'Андрей','Лебедев','+79213456780','Haval Jolion','Е789СН22','2026-05-31');
INSERT INTO "clients" VALUES (14,'Юлия','Соловьёва','+79314567891','Great Wall Poer','У951КМ44','2026-05-31');
INSERT INTO "clients" VALUES (15,'Максим','Тимофеев','+79415678902','Haval F7','А753ЕМ55','2026-05-31');
INSERT INTO "clients" VALUES (16,'Светлана','Григорьева','+79516789013','Tank 500','В159А66','2026-05-31');
INSERT INTO "clients" VALUES (17,'Никита','Степанов','+79617890124','Haval Dargo','С753РО77','2026-05-31');
INSERT INTO "clients" VALUES (18,'Екатерина','Андреева','+79718901235','Haval Jolion','М456КС88','2026-05-31');
INSERT INTO "clients" VALUES (19,'Роман','Ильин','+79819012346','Great Wall Poer','А753ВС99','2026-05-31');
INSERT INTO "clients" VALUES (20,'Тестер','Тестерович','+78887774445','Haval H9','А222АА77','');
INSERT INTO "clients" VALUES (21,'А','А','+79999999999','Haval Jolion','А111АА63','');
INSERT INTO "order_parts" VALUES ('ZAK-01/01/26-0001','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/01/26-0001','Щётка двери багажника',900.0,2,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/01/26-0001','Зеркало заднего вида левое',8500.0,2,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/01/26-0002','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/01/26-0002','Диск тормозной передний',6500.0,3,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/01/26-0002','Охлаждающая жидкость',2200.0,2,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/01/26-0003','Щётка лобового стекла правая',1200.0,3,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/01/26-0003','Термостат GWM',2200.0,1,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/01/26-0003','Стекло лобовое',25000.0,3,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/01/26-0003','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/01/26-0004','Масло моторное 0W-30',1000.0,2,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/01/26-0004','Ремень генератора GWM',1500.0,1,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/01/26-0004','Термостат GWM',2200.0,2,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/01/26-0004','Щётка лобового стекла правая',1200.0,2,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/01/26-0005','Зеркало заднего вида левое',8500.0,2,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/01/26-0005','Ремень генератора GWM',1500.0,3,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/01/26-0005','Поворотник правый',3500.0,3,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/01/26-0006','Пробка сливная картера двигателя',200.0,3,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/01/26-0006','Масло моторное 0W-30',1000.0,3,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/01/26-0006','Воздушный фильтр GWM',800.0,2,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/01/26-0006','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/01/26-0007','Колодки тормозные передние (комплект)',4000.0,3,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/01/26-0007','Фильтр салона (пылевой)',1500.0,2,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/01/26-0007','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/01/26-0008','Диск тормозной задний',5500.0,1,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/01/26-0008','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/01/26-0008','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/01/26-0008','Диск тормозной передний Dargo',6200.0,3,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/01/26-0009','Свеча накала (дизель)',1200.0,1,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/01/26-0009','Аккумулятор 60Ah',8900.0,3,1,'шт',5500.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/01/26-0009','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/01/26-0009','Фильтр масляный Haval',650.0,2,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/01/26-0010','Ремень генератора GWM',1500.0,3,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/01/26-0010','Аккумулятор 60Ah',8900.0,2,1,'шт',5500.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/01/26-0010','Фильтр масляный Haval',650.0,3,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/01/26-0011','Жидкость тормозная DOT 4',400.0,1,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/01/26-0011','Поворотник левый',3500.0,3,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/01/26-0011','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/01/26-0012','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/01/26-0012','Антифриз G11',1500.0,3,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/01/26-0012','Свечи зажигания NGK 4шт',3200.0,2,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/01/26-0012','Диск тормозной задний',5500.0,1,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/01/26-0013','Колодки тормозные передние GWM',4200.0,2,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/01/26-0013','Салонный фильтр GWM',700.0,1,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/01/26-0013','Стекло боковое правое',12000.0,3,39,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/01/26-0014','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/01/26-0014','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/01/26-0014','Колодки тормозные задние GWM',3800.0,3,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/01/26-0014','Воздушный фильтр GWM',800.0,3,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/01/26-0015','Воздушный фильтр GWM',800.0,1,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/01/26-0015','Амортизатор передний Dargo',11500.0,3,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/01/26-0015','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/01/26-0016','Колодки тормозные передние (комплект)',4000.0,2,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/01/26-0016','Шаровая опора усиленная Tank',7500.0,1,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/01/26-0016','Ремень поликлиновой',2500.0,1,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/01/26-0016','Щётка двери багажника',900.0,1,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/01/26-0017','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/01/26-0017','Пробка сливная картера двигателя',200.0,1,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/01/26-0017','Фильтр масляный Haval',650.0,1,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/01/26-0018','Ремень генератора GWM',1500.0,3,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/01/26-0018','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/01/26-0018','Воздушный фильтр GWM',800.0,1,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/01/26-0018','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/01/26-0019','Ролик направляющий',2200.0,1,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/01/26-0019','Ремень поликлиновой',2500.0,1,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/01/26-0019','Колодки тормозные задние',3500.0,2,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/01/26-0019','Пробка сливная картера двигателя',200.0,1,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/01/26-0020','Катушка зажигания GWM',4500.0,1,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/01/26-0020','Масло моторное 5W-30',1200.0,3,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/01/26-0020','Колодки тормозные задние',3500.0,3,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/01/26-0021','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/01/26-0021','Ролик натяжной ремня',2800.0,1,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/01/26-0021','Диск тормозной задний',5500.0,3,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/01/26-0022','Прокладка сливной пробки картера',100.0,3,27,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/01/26-0022','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/01/26-0022','Стекло боковое левое',12000.0,2,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/01/26-0023','Фара передняя левая',18000.0,2,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/01/26-0023','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/01/26-0023','Масло трансмиссионное ATF',1500.0,1,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/01/26-0024','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/01/26-0024','Колодки тормозные передние GWM',4200.0,1,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/01/26-0024','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/01/26-0025','Стекло лобовое',25000.0,1,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/01/26-0025','Щётка двери багажника',900.0,3,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/01/26-0025','Топливный фильтр',1200.0,3,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/01/26-0025','Салонный фильтр GWM',700.0,2,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/02/26-0026','Термостат GWM',2200.0,3,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/02/26-0026','Колодки тормозные передние GWM',4200.0,2,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/02/26-0026','Зеркало заднего вида левое',8500.0,3,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/02/26-0027','Щётка лобового стекла правая',1200.0,3,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/02/26-0027','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/02/26-0027','Стекло боковое левое',12000.0,2,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/02/26-0028','Фара передняя правая',18000.0,1,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/02/26-0028','Колодки тормозные передние (комплект)',4000.0,2,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/02/26-0028','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/02/26-0029','Аккумулятор 75Ач Exide',13500.0,2,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/02/26-0029','Зеркало заднего вида правое',8500.0,3,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/02/26-0029','Фильтр салона (пылевой)',1500.0,3,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/02/26-0029','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/02/26-0030','Колодки тормозные задние GWM',3800.0,2,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/02/26-0030','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/02/26-0030','Щётка лобового стекла левая',1200.0,1,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/02/26-0030','Ремень генератора GWM',1500.0,1,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/02/26-0031','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/02/26-0031','Топливный фильтр',1200.0,3,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/02/26-0031','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/02/26-0031','Пробка сливная картера двигателя',200.0,1,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/02/26-0032','Стекло боковое правое',12000.0,3,39,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/02/26-0032','Ремень ГРМ',3500.0,2,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/02/26-0032','Охлаждающая жидкость',2200.0,2,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/02/26-0033','Амортизатор передний Dargo',11500.0,3,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/02/26-0033','Фильтр масляный',5000.0,3,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/02/26-0033','Ремень ГРМ',3500.0,3,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/02/26-0033','Аккумулятор 60Ah',8900.0,3,1,'шт',5500.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/02/26-0034','Фильтр масляный Haval',650.0,2,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/02/26-0034','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/02/26-0034','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/02/26-0035','Фара передняя левая',18000.0,1,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/02/26-0035','Поворотник правый',3500.0,3,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/02/26-0035','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/02/26-0035','Фильтр салона (пылевой)',1500.0,2,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/02/26-0036','Ремень генератора GWM',1500.0,3,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/02/26-0036','Ремень ГРМ комплект Dargo',6200.0,2,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/02/26-0036','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/02/26-0036','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/02/26-0037','Щётка двери багажника',900.0,1,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/02/26-0037','Катушка зажигания GWM',4500.0,1,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/02/26-0037','Жидкость тормозная DOT 4',400.0,1,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/02/26-0038','Пробка сливная картера двигателя',200.0,3,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/02/26-0038','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/02/26-0038','Жидкость тормозная DOT 4',400.0,1,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/02/26-0038','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/02/26-0039','Жидкость для омывателя',600.0,3,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/02/26-0039','Щётка двери багажника',900.0,3,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/02/26-0039','Ремень генератора GWM',1500.0,3,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/02/26-0039','Фара передняя левая',18000.0,2,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/02/26-0040','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/02/26-0040','Колодки тормозные передние GWM',4200.0,2,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/02/26-0040','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/02/26-0040','Колодки тормозные передние (комплект)',4000.0,2,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/02/26-0041','Масло моторное 0W-30',1000.0,3,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/02/26-0041','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/02/26-0041','Масляный фильтр GWM',650.0,1,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/02/26-0042','Прокладка сливной пробки картера',100.0,2,27,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/02/26-0042','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/02/26-0042','Свеча зажигания',600.0,1,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/02/26-0042','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/02/26-0043','Фильтр топливный',1800.0,2,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/02/26-0043','Поворотник правый',3500.0,3,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/02/26-0043','Щётки стеклоочистителя Bosch',2200.0,3,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/02/26-0043','Щётка лобового стекла правая',1200.0,3,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/02/26-0044','Аккумулятор 75Ач Exide',13500.0,3,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/02/26-0044','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/02/26-0044','Моторное масло 5W-30 ZIC',3200.0,3,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/02/26-0044','Фара передняя левая',18000.0,1,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/02/26-0045','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/02/26-0045','Ремень ГРМ',3500.0,1,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/02/26-0045','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/02/26-0046','Фильтр топливный',1800.0,2,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/02/26-0046','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/02/26-0046','Масло моторное 0W-30',1000.0,1,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/02/26-0046','Щётка лобового стекла правая',1200.0,2,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/02/26-0047','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/02/26-0047','Катушка зажигания GWM',4500.0,3,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/02/26-0047','Щётка лобового стекла правая',1200.0,1,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/03/26-0048','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/03/26-0048','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/03/26-0048','Воздушный фильтр GWM',800.0,1,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/03/26-0048','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/03/26-0049','Зеркало заднего вида правое',8500.0,2,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/03/26-0049','Фильтр масляный Haval',650.0,3,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/03/26-0049','Салонный фильтр GWM',700.0,3,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/03/26-0049','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/03/26-0050','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/03/26-0050','Колодки тормозные передние (комплект)',4000.0,3,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/03/26-0050','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/03/26-0050','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/03/26-0051','Охлаждающая жидкость',2200.0,2,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/03/26-0051','Щётка двери багажника',900.0,2,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/03/26-0051','Колодки тормозные передние GWM',4200.0,3,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/03/26-0052','Ролик направляющий',2200.0,3,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/03/26-0052','Свеча накала (дизель)',1200.0,2,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/03/26-0052','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/03/26-0052','Колодки тормозные передние (комплект)',4000.0,3,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/03/26-0053','Пробка сливная картера двигателя',200.0,2,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/03/26-0053','Фильтр воздушный',1200.0,3,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/03/26-0053','Колодки тормозные передние (комплект)',4000.0,3,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/03/26-0054','Фильтр воздушный',1200.0,2,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/03/26-0054','Ремень генератора GWM',1500.0,1,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/03/26-0054','Масло моторное 0W-30',1000.0,1,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/03/26-0054','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/03/26-0055','Колодки тормозные задние',3500.0,2,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/03/26-0055','Фильтр салона (пылевой)',1500.0,3,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/03/26-0055','Ремень поликлиновой',2500.0,3,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/03/26-0056','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/03/26-0056','Зеркало заднего вида правое',8500.0,3,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/03/26-0056','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/03/26-0057','Фильтр воздушный',1200.0,2,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/03/26-0057','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/03/26-0057','Термостат GWM',2200.0,1,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/03/26-0057','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/03/26-0058','Колодки тормозные передние GWM',4200.0,1,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/03/26-0058','Ремень поликлиновой',2500.0,2,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/03/26-0058','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/03/26-0059','Колодки тормозные передние (комплект)',4000.0,2,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/03/26-0059','Диск тормозной передний Dargo',6200.0,3,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/03/26-0059','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/03/26-0060','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/03/26-0060','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/03/26-0060','Стекло лобовое',25000.0,1,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/03/26-0060','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/03/26-0061','Свеча накала (дизель)',1200.0,1,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/03/26-0061','Воздушный фильтр GWM',800.0,2,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/03/26-0061','Зеркало заднего вида левое',8500.0,2,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/03/26-0062','Фильтр воздушный',1200.0,1,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/03/26-0062','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/03/26-0062','Воздушный фильтр GWM',800.0,3,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/03/26-0062','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/03/26-0063','Масло моторное 0W-30',1000.0,1,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/03/26-0063','Щётка лобового стекла левая',1200.0,1,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/03/26-0063','Жидкость тормозная DOT 4',400.0,1,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/03/26-0064','Масло моторное 0W-30',1000.0,2,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/03/26-0064','Ролик натяжной ремня',2800.0,1,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/03/26-0064','Свеча зажигания',600.0,3,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/03/26-0064','Ролик направляющий',2200.0,1,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/03/26-0065','Фильтр топливный',1800.0,2,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/03/26-0065','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/03/26-0065','Щётка лобового стекла левая',1200.0,1,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/03/26-0066','Диск тормозной задний',5500.0,2,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/03/26-0066','Ремень поликлиновой',2500.0,2,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/03/26-0066','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/03/26-0066','Прокладка сливной пробки картера',100.0,1,27,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/03/26-0067','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/03/26-0067','Диск тормозной задний',5500.0,2,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/03/26-0067','Масляный фильтр GWM',650.0,1,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/03/26-0068','Антифриз G11',1500.0,3,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/03/26-0068','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/03/26-0068','Салонный фильтр GWM',700.0,3,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/03/26-0068','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/03/26-0069','Фильтр воздушный',1200.0,3,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/03/26-0069','Ролик направляющий',2200.0,3,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/03/26-0069','Диск тормозной передний Dargo',6200.0,1,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/03/26-0069','Аккумулятор 75Ач Exide',13500.0,3,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/03/26-0070','Колодки тормозные задние GWM',3800.0,2,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/03/26-0070','Щётка лобового стекла правая',1200.0,1,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/03/26-0070','Диск тормозной передний Dargo',6200.0,3,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/03/26-0071','Ремень поликлиновой',2500.0,1,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/03/26-0071','Колодки тормозные передние (комплект)',4000.0,2,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/03/26-0071','Ремень генератора GWM',1500.0,1,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/03/26-0071','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/03/26-0072','Шаровая опора усиленная Tank',7500.0,1,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/03/26-0072','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/03/26-0072','Масло моторное 0W-30',1000.0,3,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/03/26-0072','Щётка лобового стекла левая',1200.0,2,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/03/26-0073','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/03/26-0073','Моторное масло 5W-30 ZIC',3200.0,2,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/03/26-0073','Свеча накала (дизель)',1200.0,2,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/03/26-0073','Фильтр топливный',1800.0,2,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/03/26-0074','Фильтр салона (пылевой)',1500.0,2,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/03/26-0074','Ремень поликлиновой',2500.0,2,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/03/26-0074','Фильтр масляный',5000.0,3,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/03/26-0074','Щётка двери багажника',900.0,3,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/03/26-0075','Диск тормозной передний',6500.0,2,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/03/26-0075','Щётки стеклоочистителя Bosch',2200.0,1,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/03/26-0075','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/03/26-0075','Салонный фильтр GWM',700.0,1,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/04/26-0076','Ремень ГРМ',3500.0,3,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/04/26-0076','Диск тормозной передний',6500.0,3,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/04/26-0076','Фильтр топливный',1800.0,1,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/04/26-0077','Масло моторное 5W-30',1200.0,2,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/04/26-0077','Охлаждающая жидкость',2200.0,1,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/04/26-0077','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/04/26-0078','Стекло лобовое',25000.0,1,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/04/26-0078','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/04/26-0078','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/04/26-0079','Масло моторное 5W-30',1200.0,1,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/04/26-0079','Фара передняя правая',18000.0,2,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/04/26-0079','Свеча зажигания',600.0,2,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/04/26-0079','Зеркало заднего вида левое',8500.0,3,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/04/26-0080','Щётки стеклоочистителя Bosch',2200.0,2,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/04/26-0080','Ремень поликлиновой',2500.0,3,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/04/26-0080','Диск тормозной передний',6500.0,2,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/04/26-0080','Колодки тормозные передние (комплект)',4000.0,3,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/04/26-0081','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/04/26-0081','Фара передняя правая',18000.0,1,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/04/26-0081','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/04/26-0082','Стекло лобовое',25000.0,3,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/04/26-0082','Колодки тормозные передние GWM',4200.0,3,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/04/26-0082','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/04/26-0083','Стекло боковое левое',12000.0,1,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/04/26-0083','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/04/26-0083','Фильтр масляный Haval',650.0,2,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/04/26-0084','Салонный фильтр GWM',700.0,1,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/04/26-0084','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/04/26-0084','Масло моторное 0W-30',1000.0,2,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/04/26-0084','Щётка лобового стекла правая',1200.0,2,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/04/26-0085','Щётки стеклоочистителя Bosch',2200.0,1,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/04/26-0085','Щётка двери багажника',900.0,2,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/04/26-0085','Свеча накала (дизель)',1200.0,1,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/04/26-0085','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/04/26-0086','Диск тормозной передний',6500.0,1,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/04/26-0086','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/04/26-0086','Колодки тормозные задние GWM',3800.0,3,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/04/26-0086','Ремень поликлиновой',2500.0,2,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/04/26-0087','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/04/26-0087','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/04/26-0087','Амортизатор передний Dargo',11500.0,1,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/04/26-0087','Масло трансмиссионное ATF',1500.0,1,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/04/26-0088','Стекло боковое левое',12000.0,2,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/04/26-0088','Фильтр салона (пылевой)',1500.0,3,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/04/26-0088','Фильтр масляный Haval',650.0,1,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/04/26-0088','Жидкость тормозная DOT 4',400.0,2,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/04/26-0089','Ремень ГРМ комплект Dargo',6200.0,1,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/04/26-0089','Колодки тормозные передние (комплект)',4000.0,3,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/04/26-0089','Щётки стеклоочистителя Bosch',2200.0,2,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/04/26-0089','Поворотник левый',3500.0,3,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/04/26-0090','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/04/26-0090','Щётки стеклоочистителя Bosch',2200.0,1,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/04/26-0090','Щётка лобового стекла левая',1200.0,3,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/04/26-0091','Шаровая опора усиленная Tank',7500.0,1,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/04/26-0091','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/04/26-0091','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/04/26-0092','Масло моторное 0W-30',1000.0,1,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/04/26-0092','Щётки стеклоочистителя Bosch',2200.0,3,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/04/26-0092','Жидкость тормозная DOT 4',400.0,2,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/04/26-0093','Шаровая опора усиленная Tank',7500.0,1,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/04/26-0093','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/04/26-0093','Стекло боковое левое',12000.0,2,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/04/26-0093','Масляный фильтр GWM',650.0,1,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/04/26-0094','Шаровая опора передняя Dargo',5500.0,1,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/04/26-0094','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/04/26-0094','Ролик натяжной ремня',2800.0,3,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/04/26-0094','Стекло боковое левое',12000.0,1,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/04/26-0095','Щётки стеклоочистителя Bosch',2200.0,1,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/04/26-0095','Пробка сливная картера двигателя',200.0,2,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/04/26-0095','Ремень ГРМ',3500.0,3,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/04/26-0095','Фара передняя правая',18000.0,2,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/05/26-0096','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/05/26-0096','Колодки тормозные передние GWM',4200.0,2,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/05/26-0096','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/05/26-0096','Жидкость тормозная DOT 4',400.0,1,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/05/26-0097','Стекло боковое левое',12000.0,3,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/05/26-0097','Свеча накала (дизель)',1200.0,1,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/05/26-0097','Моторное масло 5W-30 ZIC',3200.0,2,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/05/26-0098','Диск тормозной передний',6500.0,1,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/05/26-0098','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/05/26-0098','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/05/26-0098','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/05/26-0099','Щётки стеклоочистителя Bosch',2200.0,3,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/05/26-0099','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/05/26-0099','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/05/26-0099','Щётка лобового стекла левая',1200.0,3,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/05/26-0100','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/05/26-0100','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/05/26-0100','Аккумулятор 60Ah',8900.0,3,1,'шт',5500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/05/26-0100','Зеркало заднего вида правое',8500.0,2,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/05/26-0101','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/05/26-0101','Прокладка сливной пробки картера',100.0,1,27,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/05/26-0101','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/05/26-0102','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/05/26-0102','Стекло лобовое',25000.0,1,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/05/26-0102','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/05/26-0103','Фильтр топливный',1800.0,2,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/05/26-0103','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/05/26-0103','Диск тормозной передний Dargo',6200.0,1,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/05/26-0104','Аккумулятор 60Ah',8900.0,1,1,'шт',5500.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/05/26-0104','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/05/26-0104','Масло трансмиссионное ATF',1500.0,2,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/05/26-0105','Зеркало заднего вида правое',8500.0,3,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/05/26-0105','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/05/26-0105','Поворотник правый',3500.0,3,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/05/26-0105','Свеча зажигания',600.0,3,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/05/26-0106','Воздушный фильтр GWM',800.0,3,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/05/26-0106','Масляный фильтр GWM',650.0,2,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/05/26-0106','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/05/26-0106','Ремень ГРМ',3500.0,3,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/05/26-0107','Зеркало заднего вида левое',8500.0,3,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/05/26-0107','Щётка двери багажника',900.0,2,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/05/26-0107','Стекло лобовое',25000.0,1,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/05/26-0107','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/05/26-0108','Масло трансмиссионное ATF',1500.0,2,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/05/26-0108','Фильтр салона (пылевой)',1500.0,1,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/05/26-0108','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/05/26-0108','Колодки тормозные задние GWM',3800.0,3,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/05/26-0109','Диск тормозной передний',6500.0,1,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/05/26-0109','Жидкость тормозная DOT 4',400.0,1,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/05/26-0109','Колодки тормозные передние GWM',4200.0,3,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/05/26-0110','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/05/26-0110','Свечи зажигания NGK 4шт',3200.0,2,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/05/26-0110','Топливный фильтр',1200.0,3,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/05/26-0110','Термостат GWM',2200.0,2,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/05/26-0111','Шаровая опора усиленная Tank',7500.0,2,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/05/26-0111','Масло моторное 0W-30',1000.0,3,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/05/26-0111','Стекло боковое левое',12000.0,1,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/05/26-0111','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/05/26-0112','Воздушный фильтр GWM',800.0,3,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/05/26-0112','Топливный фильтр',1200.0,3,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/05/26-0112','Щётки стеклоочистителя Bosch',2200.0,3,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/05/26-0112','Диск тормозной задний',5500.0,2,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/05/26-0113','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/05/26-0113','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/05/26-0113','Диск тормозной передний',6500.0,1,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/05/26-0114','Салонный фильтр GWM',700.0,2,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/05/26-0114','Стекло лобовое',25000.0,2,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/05/26-0114','Зеркало заднего вида левое',8500.0,2,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/05/26-0114','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/05/26-0115','Масло моторное 5W-30',1200.0,3,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/05/26-0115','Колодки тормозные передние GWM',4200.0,3,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/05/26-0115','Фильтр масляный Haval',650.0,1,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/05/26-0115','Щётки стеклоочистителя Bosch',2200.0,2,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/05/26-0116','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/05/26-0116','Свеча накала (дизель)',1200.0,2,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/05/26-0116','Поворотник правый',3500.0,1,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/05/26-0117','Масло трансмиссионное ATF',1500.0,2,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/05/26-0117','Зеркало заднего вида правое',8500.0,1,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/05/26-0117','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/05/26-0117','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/05/26-0118','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/05/26-0118','Моторное масло 5W-30 ZIC',3200.0,1,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/05/26-0118','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/05/26-0119','Амортизатор передний Dargo',11500.0,3,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/05/26-0119','Прокладка сливной пробки картера',100.0,1,27,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/05/26-0119','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/05/26-0119','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/05/26-0120','Щётка двери багажника',900.0,1,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/05/26-0120','Шаровая опора усиленная Tank',7500.0,2,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/05/26-0120','Ремень поликлиновой',2500.0,3,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/05/26-0120','Щётка лобового стекла правая',1200.0,3,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/05/26-0121','Зеркало заднего вида левое',8500.0,3,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/05/26-0121','Щётка лобового стекла левая',1200.0,1,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/05/26-0121','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/05/26-0121','Щётка двери багажника',900.0,1,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/05/26-0122','Масло моторное 5W-30',1200.0,1,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/05/26-0122','Диск тормозной задний',5500.0,2,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/05/26-0122','Фильтр топливный',1800.0,2,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/05/26-0123','Аккумулятор 75Ач Exide',13500.0,3,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/05/26-0123','Прокладка сливной пробки картера',100.0,2,27,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/05/26-0123','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/05/26-0123','Пробка сливная картера двигателя',200.0,2,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/05/26-0124','Щётка лобового стекла правая',1200.0,2,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/05/26-0124','Фильтр масляный',5000.0,1,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/05/26-0124','Топливный фильтр',1200.0,1,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/05/26-0125','Диск тормозной задний',5500.0,1,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/05/26-0125','Аккумулятор 75Ач Exide',13500.0,1,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/05/26-0125','Колодки тормозные передние GWM',4200.0,3,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/06/26-0126','Зеркало заднего вида правое',8500.0,2,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/06/26-0126','Фара передняя левая',18000.0,1,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/06/26-0126','Диск тормозной задний',5500.0,3,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/06/26-0126','Салонный фильтр GWM',700.0,3,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/06/26-0127','Термостат GWM',2200.0,2,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/06/26-0127','Поворотник правый',3500.0,3,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/06/26-0127','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/06/26-0128','Амортизатор передний Dargo',11500.0,2,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/06/26-0128','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/06/26-0128','Свеча зажигания',600.0,1,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/06/26-0129','Амортизатор передний Dargo',11500.0,1,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/06/26-0129','Масло моторное 5W-30',1200.0,1,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/06/26-0129','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/06/26-0129','Шаровая опора усиленная Tank',7500.0,2,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/06/26-0130','Охлаждающая жидкость',2200.0,2,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/06/26-0130','Шаровая опора передняя Dargo',5500.0,2,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/06/26-0130','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/06/26-0130','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/06/26-0131','Фильтр воздушный',1200.0,3,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/06/26-0131','Ролик направляющий',2200.0,2,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/06/26-0131','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/06/26-0131','Щётка двери багажника',900.0,3,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/06/26-0132','Диск тормозной передний',6500.0,2,7,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/06/26-0132','Диск тормозной задний',5500.0,2,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/06/26-0132','Масляный фильтр GWM',650.0,2,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/06/26-0132','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/06/26-0133','Стекло лобовое',25000.0,1,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/06/26-0133','Ремень ГРМ',3500.0,1,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/06/26-0133','Масло моторное 0W-30',1000.0,3,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/06/26-0133','Термостат GWM',2200.0,3,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/06/26-0134','Амортизатор передний Dargo',11500.0,3,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/06/26-0134','Жидкость для омывателя',600.0,2,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/06/26-0134','Воздушный фильтр GWM',800.0,2,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/06/26-0134','Колодки тормозные передние (комплект)',4000.0,2,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/06/26-0135','Ремень ГРМ комплект Dargo',6200.0,1,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/06/26-0135','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/06/26-0135','Воздушный фильтр GWM',800.0,2,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/06/26-0135','Свечи зажигания NGK 4шт',3200.0,2,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/06/26-0136','Ролик направляющий',2200.0,3,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/06/26-0136','Салонный фильтр GWM',700.0,1,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-15/06/26-0136','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/06/26-0137','Диск тормозной передний Dargo',6200.0,1,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/06/26-0137','Жидкость для омывателя',600.0,2,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-17/06/26-0137','Щётки стеклоочистителя Bosch',2200.0,1,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/06/26-0138','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/06/26-0138','Щётки стеклоочистителя Bosch',2200.0,3,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/06/26-0138','Зеркало заднего вида левое',8500.0,3,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/06/26-0138','Фильтр масляный',5000.0,1,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/06/26-0139','Колодки тормозные передние (комплект)',4000.0,1,16,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/06/26-0139','Шаровая опора усиленная Tank',7500.0,2,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/06/26-0139','Ролик направляющий',2200.0,3,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/06/26-0140','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/06/26-0140','Пробка сливная картера двигателя',200.0,2,26,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/06/26-0140','Фильтр масляный',5000.0,3,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/06/26-0141','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/06/26-0141','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/06/26-0141','Фара передняя правая',18000.0,1,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/06/26-0142','Щётка лобового стекла правая',1200.0,1,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/06/26-0142','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/06/26-0142','Салонный фильтр GWM',700.0,1,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/06/26-0143','Щётки стеклоочистителя Bosch',2200.0,3,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/06/26-0143','Диск тормозной задний',5500.0,3,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/06/26-0143','Колодки тормозные передние GWM',4200.0,2,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/06/26-0143','Катушка зажигания GWM',4500.0,1,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/06/26-0144','Зеркало заднего вида левое',8500.0,3,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/06/26-0144','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/06/26-0144','Стекло лобовое',25000.0,2,40,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/06/26-0145','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/06/26-0145','Колодки тормозные задние',3500.0,3,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/06/26-0145','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/06/26-0145','Свечи зажигания NGK 4шт',3200.0,1,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/06/26-0146','Ремень генератора GWM',1500.0,1,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/06/26-0146','Воздушный фильтр GWM',800.0,2,5,'шт',450.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/06/26-0146','Фильтр воздушный',1200.0,2,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/06/26-0146','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/06/26-0147','Жидкость для омывателя',600.0,3,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/06/26-0147','Фильтр топливный',1800.0,1,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/06/26-0147','Поворотник левый',3500.0,2,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-27/06/26-0147','Ролик натяжной ремня',2800.0,1,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/06/26-0148','Антифриз G11',1500.0,3,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/06/26-0148','Масло моторное 5W-30',1200.0,2,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/06/26-0148','Поворотник правый',3500.0,1,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/06/26-0149','Ролик направляющий',2200.0,2,32,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/06/26-0149','Масло трансмиссионное ATF',1500.0,3,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/06/26-0149','Аккумулятор 75Ач Exide',13500.0,2,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/06/26-0149','Диск тормозной задний',5500.0,1,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/07/26-0150','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/07/26-0150','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/07/26-0150','Масло моторное 0W-30',1000.0,3,18,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/07/26-0150','Щётка лобового стекла левая',1200.0,1,53,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/07/26-0151','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/07/26-0151','Стекло боковое правое',12000.0,3,39,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/07/26-0151','Ролик натяжной ремня',2800.0,1,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/07/26-0152','Жидкость тормозная DOT 4',400.0,2,10,'шт',300.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/07/26-0152','Стекло боковое правое',12000.0,3,39,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/07/26-0152','Ролик натяжной ремня',2800.0,2,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/07/26-0152','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/07/26-0153','Фильтр масляный',5000.0,1,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/07/26-0153','Свечи зажигания NGK 4шт',3200.0,2,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/07/26-0153','Масло трансмиссионное ATF',1500.0,2,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/07/26-0153','Ремень поликлиновой',2500.0,3,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/07/26-0154','Фильтр масляный Haval',650.0,2,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/07/26-0154','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/07/26-0154','Стекло боковое левое',12000.0,3,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/07/26-0155','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/07/26-0155','Аккумулятор 75Ач Exide',13500.0,2,2,'шт',8500.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/07/26-0155','Ремень ГРМ комплект Dargo',6200.0,1,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/07/26-0156','Салонный фильтр GWM',700.0,2,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/07/26-0156','Колодки тормозные задние',3500.0,3,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/07/26-0156','Свеча накала (дизель)',1200.0,2,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/07/26-0157','Свеча зажигания',600.0,3,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/07/26-0157','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/07/26-0157','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/07/26-0157','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/07/26-0158','Колодки тормозные задние GWM',3800.0,3,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/07/26-0158','Масло трансмиссионное ATF',1500.0,1,20,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/07/26-0158','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/07/26-0158','Масло моторное 5W-30',1200.0,3,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/07/26-0159','Свечи зажигания NGK 4шт',3200.0,2,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/07/26-0159','Фара передняя правая',18000.0,2,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/07/26-0159','Свеча зажигания',600.0,2,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/07/26-0160','Ремень ГРМ комплект Dargo',6200.0,2,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/07/26-0160','Топливный фильтр',1200.0,1,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/07/26-0160','Колодки тормозные передние GWM',4200.0,1,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-12/07/26-0160','Фильтр салона (пылевой)',1500.0,3,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/07/26-0161','Антифриз G11',1500.0,1,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/07/26-0161','Фара передняя левая',18000.0,3,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-13/07/26-0161','Салонный фильтр GWM',700.0,1,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/07/26-0162','Аккумулятор 60Ah',8900.0,2,1,'шт',5500.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/07/26-0162','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-14/07/26-0162','Зеркало заднего вида левое',8500.0,2,11,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/07/26-0163','Топливный фильтр',1200.0,3,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/07/26-0163','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-16/07/26-0163','Колодки тормозные задние GWM',3800.0,3,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/07/26-0164','Зеркало заднего вида правое',8500.0,1,12,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/07/26-0164','Фильтр салона (пылевой)',1500.0,2,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/07/26-0164','Масло моторное 5W-30',1200.0,3,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-18/07/26-0164','Амортизатор передний Dargo',11500.0,3,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/07/26-0165','Жидкость для омывателя',600.0,2,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/07/26-0165','Щётки стеклоочистителя Bosch',2200.0,1,55,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/07/26-0165','Ролик натяжной ремня',2800.0,1,33,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-19/07/26-0165','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/07/26-0166','Свечи зажигания NGK 4шт',3200.0,1,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/07/26-0166','Фара передняя правая',18000.0,3,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/07/26-0166','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-20/07/26-0166','Катушка зажигания GWM',4500.0,2,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/07/26-0167','Жидкость для омывателя',600.0,1,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/07/26-0167','Ремень поликлиновой',2500.0,1,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-21/07/26-0167','Масляный фильтр GWM',650.0,2,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/07/26-0168','Колодки тормозные передние GWM',4200.0,3,17,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/07/26-0168','Топливный фильтр',1200.0,1,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-22/07/26-0168','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/07/26-0169','Фильтр воздушный',1200.0,2,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/07/26-0169','Диск тормозной передний Dargo',6200.0,3,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/07/26-0169','Термостат GWM',2200.0,1,41,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-23/07/26-0169','Масло моторное 5W-30',1200.0,1,19,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/07/26-0170','Фильтр воздушный',1200.0,3,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/07/26-0170','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/07/26-0170','Ремень поликлиновой',2500.0,1,31,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-24/07/26-0170','Фильтр топливный',1800.0,1,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/07/26-0171','Антифриз G11',1500.0,3,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/07/26-0171','Щётка лобового стекла правая',1200.0,1,54,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-25/07/26-0171','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/07/26-0172','Диск тормозной передний Dargo',6200.0,3,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/07/26-0172','Фара передняя правая',18000.0,2,44,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/07/26-0172','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-26/07/26-0172','Фильтр масляный',5000.0,3,46,'шт',3000.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/07/26-0173','Свечи зажигания NGK 4шт',3200.0,3,37,'шт',1800.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/07/26-0173','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/07/26-0173','Свеча накала (дизель)',1200.0,3,36,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-28/07/26-0173','Ремень генератора GWM',1500.0,1,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/07/26-0174','Ремень ГРМ',3500.0,1,28,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/07/26-0174','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-29/07/26-0174','Фильтр воздушный',1200.0,1,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/07/26-0175','Шаровая опора передняя Dargo',5500.0,3,50,'шт',3200.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/07/26-0175','Жидкость для омывателя',600.0,3,9,'шт',500.0);
INSERT INTO "order_parts" VALUES ('ZAK-30/07/26-0175','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/07/26-0176','Топливный фильтр',1200.0,1,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/07/26-0176','Охлаждающая жидкость',2200.0,3,23,'шт',1000.0);
INSERT INTO "order_parts" VALUES ('ZAK-31/07/26-0176','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/08/26-0177','Фильтр масляный Haval',650.0,1,47,'шт',400.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/08/26-0177','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-01/08/26-0177','Диск тормозной передний Dargo',6200.0,2,8,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/08/26-0178','Стекло боковое правое',12000.0,3,39,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/08/26-0178','Амортизатор передний Dargo',11500.0,3,3,'шт',6800.0);
INSERT INTO "order_parts" VALUES ('ZAK-02/08/26-0178','Ремень ГРМ комплект Dargo',6200.0,3,29,'шт',3800.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/08/26-0179','Свеча зажигания',600.0,1,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/08/26-0179','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-03/08/26-0179','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/08/26-0180','Фара передняя левая',18000.0,2,43,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/08/26-0180','Салонный фильтр GWM',700.0,2,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-04/08/26-0180','Колодки тормозные задние GWM',3800.0,2,15,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/08/26-0181','Шаровая опора усиленная Tank',7500.0,2,51,'шт',4500.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/08/26-0181','Салонный фильтр GWM',700.0,2,34,'шт',380.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/08/26-0181','Масляный фильтр GWM',650.0,1,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-05/08/26-0181','Фильтр воздушный',1200.0,2,45,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/08/26-0182','Щётка двери багажника',900.0,3,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/08/26-0182','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-06/08/26-0182','Антифриз G11',1500.0,2,4,'шт',1200.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/08/26-0183','Колодки тормозные задние',3500.0,2,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/08/26-0183','Моторное масло 5W-30 ZIC',3200.0,2,22,'шт',2200.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/08/26-0183','Топливный фильтр',1200.0,1,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-07/08/26-0183','Диск тормозной задний',5500.0,3,6,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/08/26-0184','Щётка двери багажника',900.0,3,52,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/08/26-0184','Фильтр топливный',1800.0,3,49,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-08/08/26-0184','Стекло боковое левое',12000.0,3,38,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/08/26-0185','Колодки тормозные задние',3500.0,1,14,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/08/26-0185','Катушка зажигания GWM',4500.0,3,13,'шт',2500.0);
INSERT INTO "order_parts" VALUES ('ZAK-09/08/26-0185','Фильтр салона (пылевой)',1500.0,2,48,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/08/26-0186','Поворотник правый',3500.0,2,25,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/08/26-0186','Ремень генератора GWM',1500.0,2,30,'шт',850.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/08/26-0186','Свеча зажигания',600.0,2,35,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-10/08/26-0186','Топливный фильтр',1200.0,2,42,'шт',700.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/08/26-0187','Поворотник левый',3500.0,1,24,'шт',0.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/08/26-0187','Масляный фильтр GWM',650.0,3,21,'шт',350.0);
INSERT INTO "order_parts" VALUES ('ZAK-11/08/26-0187','Прокладка сливной пробки картера',100.0,1,27,'шт',0.0);
INSERT INTO "order_services" VALUES ('ZAK-01/01/26-0001','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-01/01/26-0001','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-02/01/26-0002','Замена сайлентблоков',5000.0,44);
INSERT INTO "order_services" VALUES ('ZAK-02/01/26-0002','Замена главного тормозного цилиндра',7000.0,14);
INSERT INTO "order_services" VALUES ('ZAK-03/01/26-0003','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-03/01/26-0003','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-04/01/26-0004','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-04/01/26-0004','Замена охлаждающей жидкости',1800.0,30);
INSERT INTO "order_services" VALUES ('ZAK-04/01/26-0004','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-05/01/26-0005','Замена масла в раздаточной коробке',2000.0,22);
INSERT INTO "order_services" VALUES ('ZAK-05/01/26-0005','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-06/01/26-0006','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-06/01/26-0006','Замена охлаждающей жидкости',1800.0,30);
INSERT INTO "order_services" VALUES ('ZAK-06/01/26-0006','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-08/01/26-0007','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-08/01/26-0007','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-08/01/26-0007','Замена помпы двигателя',10000.0,34);
INSERT INTO "order_services" VALUES ('ZAK-09/01/26-0008','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-09/01/26-0008','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-09/01/26-0008','Замена масла ДВС',1500.0,18);
INSERT INTO "order_services" VALUES ('ZAK-10/01/26-0009','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-10/01/26-0009','Кап. ремонт двигателя',100000.0,60);
INSERT INTO "order_services" VALUES ('ZAK-10/01/26-0009','Устранение течи масла',5000.0,68);
INSERT INTO "order_services" VALUES ('ZAK-12/01/26-0010','Замена прокладки поддона',12500.0,38);
INSERT INTO "order_services" VALUES ('ZAK-12/01/26-0010','Замена шаровых опор',4000.0,56);
INSERT INTO "order_services" VALUES ('ZAK-13/01/26-0011','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-13/01/26-0011','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-14/01/26-0012','Дезинфекция системы кондиционирования',2000.0,3);
INSERT INTO "order_services" VALUES ('ZAK-14/01/26-0012','Замена задних тормозных дисков',5000.0,15);
INSERT INTO "order_services" VALUES ('ZAK-14/01/26-0012','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-16/01/26-0013','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-16/01/26-0013','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-17/01/26-0014','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-17/01/26-0014','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-17/01/26-0014','Регулировка ручного тормоза',1500.0,67);
INSERT INTO "order_services" VALUES ('ZAK-18/01/26-0015','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-18/01/26-0015','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-19/01/26-0016','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-19/01/26-0016','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-21/01/26-0017','Замена тормозных шлангов',4000.0,53);
INSERT INTO "order_services" VALUES ('ZAK-21/01/26-0017','Кап. ремонт двигателя',100000.0,60);
INSERT INTO "order_services" VALUES ('ZAK-21/01/26-0017','Диагностика электрооборудования',3000.0,7);
INSERT INTO "order_services" VALUES ('ZAK-22/01/26-0018','Замена масла ДВС',1500.0,18);
INSERT INTO "order_services" VALUES ('ZAK-22/01/26-0018','Замена цепи ГРМ',60000.0,55);
INSERT INTO "order_services" VALUES ('ZAK-22/01/26-0018','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-23/01/26-0019','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-23/01/26-0019','Замена масла в раздаточной коробке',2000.0,22);
INSERT INTO "order_services" VALUES ('ZAK-24/01/26-0020','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-24/01/26-0020','Замена цепи ГРМ',60000.0,55);
INSERT INTO "order_services" VALUES ('ZAK-24/01/26-0020','Мойка двигателя',1500.0,63);
INSERT INTO "order_services" VALUES ('ZAK-26/01/26-0021','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-26/01/26-0021','Диагностика электрооборудования',3000.0,7);
INSERT INTO "order_services" VALUES ('ZAK-26/01/26-0021','Компьютерная диагностика',2000.0,62);
INSERT INTO "order_services" VALUES ('ZAK-27/01/26-0022','Замена задних тормозных колодок',2500.0,16);
INSERT INTO "order_services" VALUES ('ZAK-27/01/26-0022','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-27/01/26-0022','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-29/01/26-0023','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-29/01/26-0023','Замена помпы двигателя',10000.0,34);
INSERT INTO "order_services" VALUES ('ZAK-29/01/26-0023','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-30/01/26-0024','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-30/01/26-0024','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-31/01/26-0025','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-31/01/26-0025','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-01/02/26-0026','Аа7',12.0,1);
INSERT INTO "order_services" VALUES ('ZAK-01/02/26-0026','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-02/02/26-0027','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-02/02/26-0027','Замена масла в ДВС',2500.0,20);
INSERT INTO "order_services" VALUES ('ZAK-04/02/26-0028','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-04/02/26-0028','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-04/02/26-0028','Замена свечей зажигания',1600.0,46);
INSERT INTO "order_services" VALUES ('ZAK-05/02/26-0029','Замена масла в МКПП',5000.0,21);
INSERT INTO "order_services" VALUES ('ZAK-05/02/26-0029','Замена масла в редукторе',2000.0,23);
INSERT INTO "order_services" VALUES ('ZAK-06/02/26-0030','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-06/02/26-0030','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-07/02/26-0031','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-07/02/26-0031','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-07/02/26-0031','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-08/02/26-0032','Замена помпы двигателя',10000.0,34);
INSERT INTO "order_services" VALUES ('ZAK-08/02/26-0032','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-09/02/26-0033','Замена тормозной жидкости',2500.0,50);
INSERT INTO "order_services" VALUES ('ZAK-09/02/26-0033','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-10/02/26-0034','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-10/02/26-0034','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-10/02/26-0034','Замена распредвала',75000.0,41);
INSERT INTO "order_services" VALUES ('ZAK-11/02/26-0035','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-11/02/26-0035','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-11/02/26-0035','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-13/02/26-0036','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-13/02/26-0036','Замена моторного масла',2500.0,27);
INSERT INTO "order_services" VALUES ('ZAK-13/02/26-0036','Замена шаровых опор',4000.0,56);
INSERT INTO "order_services" VALUES ('ZAK-15/02/26-0037','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-15/02/26-0037','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-15/02/26-0037','Замена радиатора кондиционера',10000.0,40);
INSERT INTO "order_services" VALUES ('ZAK-17/02/26-0038','Замена троса ручного тормоза',3500.0,54);
INSERT INTO "order_services" VALUES ('ZAK-17/02/26-0038','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-17/02/26-0038','Замена тормозной жидкости',2500.0,50);
INSERT INTO "order_services" VALUES ('ZAK-18/02/26-0039','Диагностика электрооборудования',3000.0,7);
INSERT INTO "order_services" VALUES ('ZAK-18/02/26-0039','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-18/02/26-0039','Замена задних тормозных колодок',2500.0,16);
INSERT INTO "order_services" VALUES ('ZAK-20/02/26-0040','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-20/02/26-0040','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-20/02/26-0040','Замена масла в МКПП',5000.0,21);
INSERT INTO "order_services" VALUES ('ZAK-21/02/26-0041','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-21/02/26-0041','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-21/02/26-0041','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-22/02/26-0042','Замена масла в ДВС',2500.0,20);
INSERT INTO "order_services" VALUES ('ZAK-22/02/26-0042','Замена муфт изменения фаз ГРМ',60000.0,28);
INSERT INTO "order_services" VALUES ('ZAK-22/02/26-0042','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-24/02/26-0043','Компьютерная диагностика',2000.0,62);
INSERT INTO "order_services" VALUES ('ZAK-24/02/26-0043','Аа7',12.0,1);
INSERT INTO "order_services" VALUES ('ZAK-24/02/26-0043','Замена свечей зажигания',1600.0,46);
INSERT INTO "order_services" VALUES ('ZAK-25/02/26-0044','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-25/02/26-0044','Замена поршневых колец',125000.0,35);
INSERT INTO "order_services" VALUES ('ZAK-25/02/26-0044','Замена шаровых опор',4000.0,56);
INSERT INTO "order_services" VALUES ('ZAK-26/02/26-0045','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-26/02/26-0045','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-27/02/26-0046','Замена муфт изменения фаз ГРМ',60000.0,28);
INSERT INTO "order_services" VALUES ('ZAK-27/02/26-0046','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-27/02/26-0046','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-28/02/26-0047','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-28/02/26-0047','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-28/02/26-0047','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-01/03/26-0048','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-01/03/26-0048','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-02/03/26-0049','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-02/03/26-0049','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-02/03/26-0049','Замена масла в ДВС',2500.0,20);
INSERT INTO "order_services" VALUES ('ZAK-04/03/26-0050','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-04/03/26-0050','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-04/03/26-0050','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-05/03/26-0051','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-05/03/26-0051','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-05/03/26-0051','Мойка двигателя',1500.0,63);
INSERT INTO "order_services" VALUES ('ZAK-06/03/26-0052','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-06/03/26-0052','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-07/03/26-0053','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-07/03/26-0053','Замена прокладки поддона',12500.0,38);
INSERT INTO "order_services" VALUES ('ZAK-08/03/26-0054','Замена цепи ГРМ',60000.0,55);
INSERT INTO "order_services" VALUES ('ZAK-08/03/26-0054','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-08/03/26-0054','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-09/03/26-0055','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-09/03/26-0055','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-10/03/26-0056','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-10/03/26-0056','Замена прокладки поддона',12500.0,38);
INSERT INTO "order_services" VALUES ('ZAK-11/03/26-0057','Диагностика электрооборудования',3000.0,7);
INSERT INTO "order_services" VALUES ('ZAK-11/03/26-0057','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-12/03/26-0058','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-12/03/26-0058','Замена сайлентблоков',5000.0,44);
INSERT INTO "order_services" VALUES ('ZAK-12/03/26-0058','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-13/03/26-0059','Замена масла в ДВС',2500.0,20);
INSERT INTO "order_services" VALUES ('ZAK-13/03/26-0059','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-14/03/26-0060','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-14/03/26-0060','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-14/03/26-0060','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-15/03/26-0061','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-15/03/26-0061','Замена масла в МКПП',5000.0,21);
INSERT INTO "order_services" VALUES ('ZAK-15/03/26-0061','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-16/03/26-0062','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-16/03/26-0062','Замена охлаждающей жидкости',1800.0,30);
INSERT INTO "order_services" VALUES ('ZAK-16/03/26-0062','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-17/03/26-0063','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-17/03/26-0063','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-19/03/26-0064','Замена задних тормозных колодок',2500.0,16);
INSERT INTO "order_services" VALUES ('ZAK-19/03/26-0064','Кап. ремонт двигателя',100000.0,60);
INSERT INTO "order_services" VALUES ('ZAK-19/03/26-0064','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-20/03/26-0065','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-20/03/26-0065','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-20/03/26-0065','Замена охлаждающей жидкости',1800.0,30);
INSERT INTO "order_services" VALUES ('ZAK-21/03/26-0066','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-21/03/26-0066','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-21/03/26-0066','Замена шаровых опор',4000.0,56);
INSERT INTO "order_services" VALUES ('ZAK-22/03/26-0067','Полировка кузова',8000.0,64);
INSERT INTO "order_services" VALUES ('ZAK-22/03/26-0067','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-22/03/26-0067','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-23/03/26-0068','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-23/03/26-0068','Кап. ремонт двигателя',100000.0,60);
INSERT INTO "order_services" VALUES ('ZAK-24/03/26-0069','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-24/03/26-0069','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-24/03/26-0069','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-25/03/26-0070','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-25/03/26-0070','Замена тормозных шлангов',4000.0,53);
INSERT INTO "order_services" VALUES ('ZAK-26/03/26-0071','Развал-схождение',2000.0,66);
INSERT INTO "order_services" VALUES ('ZAK-26/03/26-0071','Замена поршневых колец',125000.0,35);
INSERT INTO "order_services" VALUES ('ZAK-26/03/26-0071','Замена троса ручного тормоза',3500.0,54);
INSERT INTO "order_services" VALUES ('ZAK-27/03/26-0072','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-27/03/26-0072','Замена троса ручного тормоза',3500.0,54);
INSERT INTO "order_services" VALUES ('ZAK-28/03/26-0073','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-28/03/26-0073','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-29/03/26-0074','Замена охлаждающей жидкости',1800.0,30);
INSERT INTO "order_services" VALUES ('ZAK-29/03/26-0074','Замена тормозных шлангов',4000.0,53);
INSERT INTO "order_services" VALUES ('ZAK-29/03/26-0074','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-30/03/26-0075','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-30/03/26-0075','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-03/04/26-0076','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-03/04/26-0076','Замена охлаждающей жидкости',1800.0,30);
INSERT INTO "order_services" VALUES ('ZAK-05/04/26-0077','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-05/04/26-0077','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-05/04/26-0077','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-08/04/26-0078','Замена масла в раздаточной коробке',2000.0,22);
INSERT INTO "order_services" VALUES ('ZAK-08/04/26-0078','Замена масла в редукторе',2000.0,23);
INSERT INTO "order_services" VALUES ('ZAK-09/04/26-0079','Замена передних тормозных колодок',2500.0,33);
INSERT INTO "order_services" VALUES ('ZAK-09/04/26-0079','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-09/04/26-0079','Балансировка колес',1000.0,2);
INSERT INTO "order_services" VALUES ('ZAK-11/04/26-0080','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-11/04/26-0080','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-11/04/26-0080','Замена масляного фильтра',800.0,26);
INSERT INTO "order_services" VALUES ('ZAK-12/04/26-0081','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-12/04/26-0081','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-13/04/26-0082','Замена свечей зажигания',1600.0,46);
INSERT INTO "order_services" VALUES ('ZAK-13/04/26-0082','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-14/04/26-0083','Замена радиатора кондиционера',10000.0,40);
INSERT INTO "order_services" VALUES ('ZAK-14/04/26-0083','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-16/04/26-0084','Замена масляного фильтра',800.0,26);
INSERT INTO "order_services" VALUES ('ZAK-16/04/26-0084','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-17/04/26-0085','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-17/04/26-0085','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-17/04/26-0085','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-18/04/26-0086','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-18/04/26-0086','Замена помпы двигателя',10000.0,34);
INSERT INTO "order_services" VALUES ('ZAK-18/04/26-0086','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-19/04/26-0087','Кап. ремонт двигателя',100000.0,60);
INSERT INTO "order_services" VALUES ('ZAK-19/04/26-0087','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-20/04/26-0088','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-20/04/26-0088','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-20/04/26-0088','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-21/04/26-0089','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-21/04/26-0089','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-21/04/26-0089','Замена муфт изменения фаз ГРМ',60000.0,28);
INSERT INTO "order_services" VALUES ('ZAK-23/04/26-0090','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-23/04/26-0090','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-24/04/26-0091','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-24/04/26-0091','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-25/04/26-0092','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-25/04/26-0092','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-26/04/26-0093','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-26/04/26-0093','Балансировка колес',1000.0,2);
INSERT INTO "order_services" VALUES ('ZAK-28/04/26-0094','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-28/04/26-0094','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-29/04/26-0095','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-29/04/26-0095','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-29/04/26-0095','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-02/05/26-0096','Замена распредвала',75000.0,41);
INSERT INTO "order_services" VALUES ('ZAK-02/05/26-0096','Замена цепи ГРМ',60000.0,55);
INSERT INTO "order_services" VALUES ('ZAK-03/05/26-0097','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-03/05/26-0097','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-03/05/26-0097','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-04/05/26-0098','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-04/05/26-0098','Замена радиатора кондиционера',10000.0,40);
INSERT INTO "order_services" VALUES ('ZAK-05/05/26-0099','Замена задних тормозных дисков',5000.0,15);
INSERT INTO "order_services" VALUES ('ZAK-05/05/26-0099','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-05/05/26-0099','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-06/05/26-0100','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-06/05/26-0100','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-06/05/26-0100','Замена поршневых колец',125000.0,35);
INSERT INTO "order_services" VALUES ('ZAK-07/05/26-0101','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-07/05/26-0101','Устранение течи масла',5000.0,68);
INSERT INTO "order_services" VALUES ('ZAK-08/05/26-0102','Диагностика электрооборудования',3000.0,7);
INSERT INTO "order_services" VALUES ('ZAK-08/05/26-0102','Замена масла в ДВС',2500.0,20);
INSERT INTO "order_services" VALUES ('ZAK-09/05/26-0103','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-09/05/26-0103','Замена троса ручного тормоза',3500.0,54);
INSERT INTO "order_services" VALUES ('ZAK-09/05/26-0103','Замена поршневых колец',125000.0,35);
INSERT INTO "order_services" VALUES ('ZAK-10/05/26-0104','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-10/05/26-0104','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-10/05/26-0104','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-11/05/26-0105','Аа7',12.0,1);
INSERT INTO "order_services" VALUES ('ZAK-11/05/26-0105','Полировка кузова',8000.0,64);
INSERT INTO "order_services" VALUES ('ZAK-12/05/26-0106','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-12/05/26-0106','Замена шаровых опор',4000.0,56);
INSERT INTO "order_services" VALUES ('ZAK-12/05/26-0106','Дезинфекция системы кондиционирования',2000.0,3);
INSERT INTO "order_services" VALUES ('ZAK-13/05/26-0107','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-13/05/26-0107','Дезинфекция системы кондиционирования',2000.0,3);
INSERT INTO "order_services" VALUES ('ZAK-14/05/26-0108','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-14/05/26-0108','Устранение течи масла',5000.0,68);
INSERT INTO "order_services" VALUES ('ZAK-14/05/26-0108','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-15/05/26-0109','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-15/05/26-0109','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-16/05/26-0110','Полировка кузова',8000.0,64);
INSERT INTO "order_services" VALUES ('ZAK-16/05/26-0110','Кап. ремонт двигателя',100000.0,60);
INSERT INTO "order_services" VALUES ('ZAK-17/05/26-0111','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-17/05/26-0111','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-18/05/26-0112','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-18/05/26-0112','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-19/05/26-0113','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-19/05/26-0113','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-19/05/26-0113','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-20/05/26-0114','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-20/05/26-0114','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-21/05/26-0115','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-21/05/26-0115','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-21/05/26-0115','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-22/05/26-0116','Замена радиатора кондиционера',10000.0,40);
INSERT INTO "order_services" VALUES ('ZAK-22/05/26-0116','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-22/05/26-0116','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-23/05/26-0117','Развал-схождение',2000.0,66);
INSERT INTO "order_services" VALUES ('ZAK-23/05/26-0117','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-23/05/26-0117','Дезинфекция системы кондиционирования',2000.0,3);
INSERT INTO "order_services" VALUES ('ZAK-24/05/26-0118','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-24/05/26-0118','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-24/05/26-0118','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-25/05/26-0119','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-25/05/26-0119','Замена масла в раздаточной коробке',2000.0,22);
INSERT INTO "order_services" VALUES ('ZAK-26/05/26-0120','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-26/05/26-0120','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-27/05/26-0121','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-27/05/26-0121','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-27/05/26-0121','Устранение течи масла',5000.0,68);
INSERT INTO "order_services" VALUES ('ZAK-28/05/26-0122','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-28/05/26-0122','Замена главного тормозного цилиндра',7000.0,14);
INSERT INTO "order_services" VALUES ('ZAK-28/05/26-0122','Регулировка ручного тормоза',1500.0,67);
INSERT INTO "order_services" VALUES ('ZAK-29/05/26-0123','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-29/05/26-0123','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-29/05/26-0123','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-30/05/26-0124','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-30/05/26-0124','Замена распредвала',75000.0,41);
INSERT INTO "order_services" VALUES ('ZAK-31/05/26-0125','Замена масла ДВС',1500.0,18);
INSERT INTO "order_services" VALUES ('ZAK-31/05/26-0125','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-31/05/26-0125','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-01/06/26-0126','Аа7',12.0,1);
INSERT INTO "order_services" VALUES ('ZAK-01/06/26-0126','Замена свечей зажигания',1600.0,46);
INSERT INTO "order_services" VALUES ('ZAK-01/06/26-0126','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-02/06/26-0127','Замена тормозной жидкости',2500.0,50);
INSERT INTO "order_services" VALUES ('ZAK-02/06/26-0127','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-03/06/26-0128','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-03/06/26-0128','Замена главного тормозного цилиндра',7000.0,14);
INSERT INTO "order_services" VALUES ('ZAK-03/06/26-0128','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-04/06/26-0129','Замена масла ДВС',1500.0,18);
INSERT INTO "order_services" VALUES ('ZAK-04/06/26-0129','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-06/06/26-0130','Замена передних тормозных колодок',2500.0,33);
INSERT INTO "order_services" VALUES ('ZAK-06/06/26-0130','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-06/06/26-0130','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-08/06/26-0131','Замена тормозной жидкости',2500.0,50);
INSERT INTO "order_services" VALUES ('ZAK-08/06/26-0131','Замена сайлентблоков',5000.0,44);
INSERT INTO "order_services" VALUES ('ZAK-08/06/26-0131','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-09/06/26-0132','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-09/06/26-0132','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-09/06/26-0132','Замена прокладки поддона',12500.0,38);
INSERT INTO "order_services" VALUES ('ZAK-10/06/26-0133','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-10/06/26-0133','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-10/06/26-0133','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-11/06/26-0134','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-11/06/26-0134','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-14/06/26-0135','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-14/06/26-0135','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-15/06/26-0136','Замена задних тормозных дисков',5000.0,15);
INSERT INTO "order_services" VALUES ('ZAK-15/06/26-0136','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-17/06/26-0137','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-17/06/26-0137','Мойка двигателя',1500.0,63);
INSERT INTO "order_services" VALUES ('ZAK-18/06/26-0138','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-18/06/26-0138','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-18/06/26-0138','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-19/06/26-0139','Замена задних тормозных колодок',2500.0,16);
INSERT INTO "order_services" VALUES ('ZAK-19/06/26-0139','Замена масляного фильтра',800.0,26);
INSERT INTO "order_services" VALUES ('ZAK-20/06/26-0140','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-20/06/26-0140','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-20/06/26-0140','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-21/06/26-0141','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-21/06/26-0141','Замена распредвала',75000.0,41);
INSERT INTO "order_services" VALUES ('ZAK-22/06/26-0142','Замена шаровых опор',4000.0,56);
INSERT INTO "order_services" VALUES ('ZAK-22/06/26-0142','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-23/06/26-0143','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-23/06/26-0143','Регулировка ручного тормоза',1500.0,67);
INSERT INTO "order_services" VALUES ('ZAK-24/06/26-0144','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-24/06/26-0144','Замена масла в раздаточной коробке',2000.0,22);
INSERT INTO "order_services" VALUES ('ZAK-25/06/26-0145','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-25/06/26-0145','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-26/06/26-0146','Компьютерная диагностика',2000.0,62);
INSERT INTO "order_services" VALUES ('ZAK-26/06/26-0146','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-26/06/26-0146','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-27/06/26-0147','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-27/06/26-0147','Замена распредвала',75000.0,41);
INSERT INTO "order_services" VALUES ('ZAK-27/06/26-0147','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-29/06/26-0148','Замер компрессии',4000.0,58);
INSERT INTO "order_services" VALUES ('ZAK-29/06/26-0148','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-29/06/26-0148','Замена масла в АКПП',7500.0,19);
INSERT INTO "order_services" VALUES ('ZAK-30/06/26-0149','Замена масляного насоса',6500.0,25);
INSERT INTO "order_services" VALUES ('ZAK-30/06/26-0149','Замена масла в МКПП',5000.0,21);
INSERT INTO "order_services" VALUES ('ZAK-30/06/26-0149','Полировка кузова',8000.0,64);
INSERT INTO "order_services" VALUES ('ZAK-01/07/26-0150','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-01/07/26-0150','Замена тормозной жидкости',2500.0,50);
INSERT INTO "order_services" VALUES ('ZAK-01/07/26-0150','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-02/07/26-0151','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-02/07/26-0151','Замена сайлентблоков',5000.0,44);
INSERT INTO "order_services" VALUES ('ZAK-02/07/26-0151','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-03/07/26-0152','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-03/07/26-0152','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-04/07/26-0153','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-04/07/26-0153','Развал-схождение',2000.0,66);
INSERT INTO "order_services" VALUES ('ZAK-04/07/26-0153','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-05/07/26-0154','Диагностика подвески',2500.0,4);
INSERT INTO "order_services" VALUES ('ZAK-05/07/26-0154','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-05/07/26-0154','Замена помпы двигателя',10000.0,34);
INSERT INTO "order_services" VALUES ('ZAK-06/07/26-0155','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-06/07/26-0155','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-07/07/26-0156','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-07/07/26-0156','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-07/07/26-0156','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-09/07/26-0157','Замена масла в редукторе',2000.0,23);
INSERT INTO "order_services" VALUES ('ZAK-09/07/26-0157','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-09/07/26-0157','Химчистка салона',5000.0,69);
INSERT INTO "order_services" VALUES ('ZAK-10/07/26-0158','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-10/07/26-0158','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-11/07/26-0159','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-11/07/26-0159','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-11/07/26-0159','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-12/07/26-0160','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-12/07/26-0160','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-13/07/26-0161','Замена главного тормозного цилиндра',7000.0,14);
INSERT INTO "order_services" VALUES ('ZAK-13/07/26-0161','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-14/07/26-0162','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-14/07/26-0162','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-14/07/26-0162','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-16/07/26-0163','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-16/07/26-0163','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-16/07/26-0163','Замена аккумулятора',1500.0,8);
INSERT INTO "order_services" VALUES ('ZAK-18/07/26-0164','Замена масла в редукторе',2000.0,23);
INSERT INTO "order_services" VALUES ('ZAK-18/07/26-0164','Диагностика тормозной системы',2500.0,5);
INSERT INTO "order_services" VALUES ('ZAK-18/07/26-0164','Замена тормозных колодок (зад)',2400.0,51);
INSERT INTO "order_services" VALUES ('ZAK-19/07/26-0165','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-19/07/26-0165','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-19/07/26-0165','Устранение течи масла',5000.0,68);
INSERT INTO "order_services" VALUES ('ZAK-20/07/26-0166','Замена маслоотделителя',75000.0,24);
INSERT INTO "order_services" VALUES ('ZAK-20/07/26-0166','Балансировка колес',1000.0,2);
INSERT INTO "order_services" VALUES ('ZAK-20/07/26-0166','Замена сайлентблоков',5000.0,44);
INSERT INTO "order_services" VALUES ('ZAK-21/07/26-0167','Замена генератора',8000.0,12);
INSERT INTO "order_services" VALUES ('ZAK-21/07/26-0167','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-21/07/26-0167','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-22/07/26-0168','Замена поршневых колец',125000.0,35);
INSERT INTO "order_services" VALUES ('ZAK-22/07/26-0168','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-22/07/26-0168','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-23/07/26-0169','Замена масла в МКПП',5000.0,21);
INSERT INTO "order_services" VALUES ('ZAK-23/07/26-0169','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-23/07/26-0169','Прокачка тормозной системы',1500.0,65);
INSERT INTO "order_services" VALUES ('ZAK-24/07/26-0170','Заправка кондиционера',3000.0,59);
INSERT INTO "order_services" VALUES ('ZAK-24/07/26-0170','Замена суппорта',5000.0,48);
INSERT INTO "order_services" VALUES ('ZAK-25/07/26-0171','Устранение течи масла',5000.0,68);
INSERT INTO "order_services" VALUES ('ZAK-25/07/26-0171','Замена стартера',7000.0,47);
INSERT INTO "order_services" VALUES ('ZAK-25/07/26-0171','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-26/07/26-0172','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-26/07/26-0172','Регулировка ручного тормоза',1500.0,67);
INSERT INTO "order_services" VALUES ('ZAK-28/07/26-0173','Замена компрессора кондиционера',15000.0,17);
INSERT INTO "order_services" VALUES ('ZAK-28/07/26-0173','Развал-схождение',2000.0,66);
INSERT INTO "order_services" VALUES ('ZAK-28/07/26-0173','Замена троса ручного тормоза',3500.0,54);
INSERT INTO "order_services" VALUES ('ZAK-29/07/26-0174','Замена муфт изменения фаз ГРМ',60000.0,28);
INSERT INTO "order_services" VALUES ('ZAK-29/07/26-0174','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-30/07/26-0175','Замена прокладки клапанной крышки',6000.0,37);
INSERT INTO "order_services" VALUES ('ZAK-30/07/26-0175','Замена тормозных шлангов',4000.0,53);
INSERT INTO "order_services" VALUES ('ZAK-31/07/26-0176','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-31/07/26-0176','Замена воздушного фильтра',800.0,11);
INSERT INTO "order_services" VALUES ('ZAK-31/07/26-0176','Шиномонтаж (1 колесо)',800.0,70);
INSERT INTO "order_services" VALUES ('ZAK-01/08/26-0177','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-01/08/26-0177','Замена масла в ДВС',2500.0,20);
INSERT INTO "order_services" VALUES ('ZAK-01/08/26-0177','Замена приводных ремней',2000.0,36);
INSERT INTO "order_services" VALUES ('ZAK-02/08/26-0178','Замена моторного масла',2500.0,27);
INSERT INTO "order_services" VALUES ('ZAK-02/08/26-0178','Замена амортизаторов задних',8000.0,9);
INSERT INTO "order_services" VALUES ('ZAK-03/08/26-0179','Замена тормозных колодок (перед)',3200.0,52);
INSERT INTO "order_services" VALUES ('ZAK-03/08/26-0179','Замена щеток стеклоочистителя',500.0,57);
INSERT INTO "order_services" VALUES ('ZAK-04/08/26-0180','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-04/08/26-0180','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-04/08/26-0180','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-05/08/26-0181','Полировка кузова',8000.0,64);
INSERT INTO "order_services" VALUES ('ZAK-05/08/26-0181','Диагностика электрооборудования',3000.0,7);
INSERT INTO "order_services" VALUES ('ZAK-05/08/26-0181','Замена салонного фильтра',1000.0,45);
INSERT INTO "order_services" VALUES ('ZAK-06/08/26-0182','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-06/08/26-0182','Замена опор двигателя',7500.0,29);
INSERT INTO "order_services" VALUES ('ZAK-06/08/26-0182','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-07/08/26-0183','Замена тормозной жидкости',2500.0,50);
INSERT INTO "order_services" VALUES ('ZAK-07/08/26-0183','Замена передних тормозных дисков',5000.0,32);
INSERT INTO "order_services" VALUES ('ZAK-08/08/26-0184','Замена топливного фильтра',1500.0,49);
INSERT INTO "order_services" VALUES ('ZAK-08/08/26-0184','Диагностика ходовой',1200.0,6);
INSERT INTO "order_services" VALUES ('ZAK-08/08/26-0184','Замена амортизаторов передних',8000.0,10);
INSERT INTO "order_services" VALUES ('ZAK-09/08/26-0185','Замена рулевых наконечников',4000.0,43);
INSERT INTO "order_services" VALUES ('ZAK-09/08/26-0185','Замена пружин подвески',6000.0,39);
INSERT INTO "order_services" VALUES ('ZAK-10/08/26-0186','Замена охлаждающей жидкости (антифриз)',3500.0,31);
INSERT INTO "order_services" VALUES ('ZAK-10/08/26-0186','Эндоскопия цилиндров',5000.0,71);
INSERT INTO "order_services" VALUES ('ZAK-10/08/26-0186','Замена ремня ГРМ',5500.0,42);
INSERT INTO "order_services" VALUES ('ZAK-11/08/26-0187','Замена гидрокомпенсаторов',80000.0,13);
INSERT INTO "order_services" VALUES ('ZAK-11/08/26-0187','Комплексная диагностика двигателя',5000.0,61);
INSERT INTO "order_services" VALUES ('ZAK-11/08/26-0187','Замена распредвала',75000.0,41);
INSERT INTO "orders" VALUES ('ZAK-01/01/26-0001',21,'closed',30200.0,'2026-01-01',119974,'2026-01-01','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-02/01/26-0002',9,'closed',42900.0,'2026-01-02',50758,'2026-01-02','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-03/01/26-0003',2,'closed',86600.0,'2026-01-03',130435,'2026-01-03','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-04/01/26-0004',2,'closed',92900.0,'2026-01-04',69742,'2026-01-04','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-05/01/26-0005',7,'closed',39000.0,'2026-01-05',79986,'2026-01-05','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-06/01/26-0006',18,'closed',14500.0,'2026-01-06',67570,'2026-01-06','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-08/01/26-0007',21,'closed',39100.0,'2026-01-08',140870,'2026-01-08','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-09/01/26-0008',19,'closed',47000.0,'2026-01-09',122311,'2026-01-09','Закрытый заказ','Great Wall Poer','А753ВС99');
INSERT INTO "orders" VALUES ('ZAK-10/01/26-0009',14,'closed',189700.0,'2026-01-10',26653,'2026-01-10','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-12/01/26-0010',21,'closed',40750.0,'2026-01-12',99174,'2026-01-12','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-13/01/26-0011',6,'closed',23800.0,'2026-01-13',143085,'2026-01-13','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-14/01/26-0012',1,'closed',34400.0,'2026-01-14',94975,'2026-01-14','Закрытый заказ','Haval Dargo','А123ВС163');
INSERT INTO "orders" VALUES ('ZAK-16/01/26-0013',18,'closed',55100.0,'2026-01-16',42967,'2026-01-16','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-17/01/26-0014',7,'closed',37200.0,'2026-01-17',91714,'2026-01-17','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-18/01/26-0015',8,'closed',48000.0,'2026-01-18',67728,'2026-01-18','Закрытый заказ','Haval Jolion','Т357ХО18');
INSERT INTO "orders" VALUES ('ZAK-19/01/26-0016',9,'closed',34700.0,'2026-01-19',137248,'2026-01-19','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-21/01/26-0017',14,'closed',110850.0,'2026-01-21',102876,'2026-01-21','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-22/01/26-0018',7,'closed',164400.0,'2026-01-22',59862,'2026-01-22','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-23/01/26-0019',18,'closed',17400.0,'2026-01-23',13868,'2026-01-23','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-24/01/26-0020',13,'closed',86100.0,'2026-01-24',79521,'2026-01-24','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-26/01/26-0021',18,'closed',27300.0,'2026-01-26',25979,'2026-01-26','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-27/01/26-0022',3,'closed',87000.0,'2026-01-27',71657,'2026-01-27','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-29/01/26-0023',9,'closed',55800.0,'2026-01-29',63545,'2026-01-29','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-30/01/26-0024',1,'closed',23400.0,'2026-01-30',130136,'2026-01-30','Закрытый заказ','Haval Dargo','А123ВС163');
INSERT INTO "orders" VALUES ('ZAK-31/01/26-0025',12,'closed',34000.0,'2026-01-31',84707,'2026-01-31','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-01/02/26-0026',14,'closed',44512.0,'2026-02-01',82530,'2026-02-01','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-02/02/26-0027',14,'closed',41600.0,'2026-02-02',12534,'2026-02-02','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-04/02/26-0028',5,'closed',89800.0,'2026-02-04',20965,'2026-02-04','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-05/02/26-0029',14,'closed',67200.0,'2026-02-05',50515,'2026-02-05','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-06/02/26-0030',9,'closed',23300.0,'2026-02-06',51732,'2026-02-06','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-07/02/26-0031',1,'closed',30000.0,'2026-02-07',60627,'2026-02-07','Закрытый заказ','Haval Dargo','А123ВС163');
INSERT INTO "orders" VALUES ('ZAK-08/02/26-0032',4,'closed',59900.0,'2026-02-08',78475,'2026-02-08','Закрытый заказ','Great Wall Poer','О159РТ40');
INSERT INTO "orders" VALUES ('ZAK-09/02/26-0033',20,'closed',95700.0,'2026-02-09',144066,'2026-02-09','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-10/02/26-0034',7,'closed',98400.0,'2026-02-10',105478,'2026-02-10','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-11/02/26-0035',11,'closed',112000.0,'2026-02-11',115486,'2026-02-11','Закрытый заказ','Haval Dargo','К123МР77');
INSERT INTO "orders" VALUES ('ZAK-13/02/26-0036',13,'closed',86900.0,'2026-02-13',10106,'2026-02-13','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-15/02/26-0037',3,'closed',27300.0,'2026-02-15',84392,'2026-02-15','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-17/02/26-0038',16,'closed',19900.0,'2026-02-17',29091,'2026-02-17','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-18/02/26-0039',8,'closed',55500.0,'2026-02-18',56107,'2026-02-18','Закрытый заказ','Haval Jolion','Т357ХО18');
INSERT INTO "orders" VALUES ('ZAK-20/02/26-0040',15,'closed',54400.0,'2026-02-20',142324,'2026-02-20','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-21/02/26-0041',21,'closed',21350.0,'2026-02-21',72716,'2026-02-21','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-22/02/26-0042',5,'closed',77500.0,'2026-02-22',70623,'2026-02-22','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-24/02/26-0043',13,'closed',27912.0,'2026-02-24',15120,'2026-02-24','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-25/02/26-0044',18,'closed',211600.0,'2026-02-25',67812,'2026-02-25','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-26/02/26-0045',15,'closed',11400.0,'2026-02-26',43456,'2026-02-26','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-27/02/26-0046',9,'closed',82500.0,'2026-02-27',109379,'2026-02-27','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-28/02/26-0047',1,'closed',35400.0,'2026-02-28',23653,'2026-02-28','Закрытый заказ','Haval Dargo','А123ВС163');
INSERT INTO "orders" VALUES ('ZAK-01/03/26-0048',10,'closed',73400.0,'2026-03-01',41733,'2026-03-01','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-02/03/26-0049',16,'closed',35950.0,'2026-03-02',37666,'2026-03-02','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-04/03/26-0050',15,'closed',57500.0,'2026-03-04',124183,'2026-03-04','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-05/03/26-0051',11,'closed',100800.0,'2026-03-05',17523,'2026-03-05','Закрытый заказ','Haval Dargo','К123МР77');
INSERT INTO "orders" VALUES ('ZAK-06/03/26-0052',9,'closed',30700.0,'2026-03-06',12661,'2026-03-06','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-07/03/26-0053',16,'closed',36500.0,'2026-03-07',127486,'2026-03-07','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-08/03/26-0054',12,'closed',78400.0,'2026-03-08',134062,'2026-03-08','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-09/03/26-0055',7,'closed',99000.0,'2026-03-09',92719,'2026-03-09','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-10/03/26-0056',17,'closed',52500.0,'2026-03-10',84186,'2026-03-10','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-11/03/26-0057',5,'closed',18600.0,'2026-03-11',81909,'2026-03-11','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-12/03/26-0058',10,'closed',102100.0,'2026-03-12',133049,'2026-03-12','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-13/03/26-0059',3,'closed',38700.0,'2026-03-13',24053,'2026-03-13','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-14/03/26-0060',20,'closed',51100.0,'2026-03-14',69162,'2026-03-14','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-15/03/26-0061',21,'closed',29100.0,'2026-03-15',65318,'2026-03-15','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-16/03/26-0062',20,'closed',70900.0,'2026-03-16',133184,'2026-03-16','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-17/03/26-0063',7,'closed',10900.0,'2026-03-17',121447,'2026-03-17','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-19/03/26-0064',10,'closed',117300.0,'2026-03-19',85657,'2026-03-19','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-20/03/26-0065',14,'closed',71100.0,'2026-03-20',94494,'2026-03-20','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-21/03/26-0066',6,'closed',103700.0,'2026-03-21',133338,'2026-03-21','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-22/03/26-0067',6,'closed',31850.0,'2026-03-22',96456,'2026-03-22','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-23/03/26-0068',13,'closed',123400.0,'2026-03-23',144898,'2026-03-23','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-24/03/26-0069',2,'closed',82900.0,'2026-03-24',63361,'2026-03-24','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-25/03/26-0070',6,'closed',36400.0,'2026-03-25',91461,'2026-03-25','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-26/03/26-0071',5,'closed',146100.0,'2026-03-26',140646,'2026-03-26','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-27/03/26-0072',17,'closed',24000.0,'2026-03-27',45784,'2026-03-27','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-28/03/26-0073',19,'closed',28000.0,'2026-03-28',138356,'2026-03-28','Закрытый заказ','Great Wall Poer','А753ВС99');
INSERT INTO "orders" VALUES ('ZAK-29/03/26-0074',7,'closed',32700.0,'2026-03-29',72601,'2026-03-29','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-30/03/26-0075',5,'closed',51000.0,'2026-03-30',139847,'2026-03-30','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-03/04/26-0076',2,'closed',39600.0,'2026-04-03',27936,'2026-04-03','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-05/04/26-0077',15,'closed',17300.0,'2026-04-05',53611,'2026-04-05','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-08/04/26-0078',17,'closed',90000.0,'2026-04-08',118016,'2026-04-08','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-09/04/26-0079',5,'closed',70400.0,'2026-04-09',70866,'2026-04-09','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-11/04/26-0080',10,'closed',46700.0,'2026-04-11',121230,'2026-04-11','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-12/04/26-0081',10,'closed',48500.0,'2026-04-12',87392,'2026-04-12','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-13/04/26-0082',16,'closed',150700.0,'2026-04-13',85024,'2026-04-13','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-14/04/26-0083',5,'closed',27300.0,'2026-04-14',35470,'2026-04-14','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-16/04/26-0084',11,'closed',26500.0,'2026-04-16',118921,'2026-04-16','Закрытый заказ','Haval Dargo','К123МР77');
INSERT INTO "orders" VALUES ('ZAK-17/04/26-0085',11,'closed',35200.0,'2026-04-17',40191,'2026-04-17','Закрытый заказ','Haval Dargo','К123МР77');
INSERT INTO "orders" VALUES ('ZAK-18/04/26-0086',18,'closed',93400.0,'2026-04-18',42398,'2026-04-18','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-19/04/26-0087',20,'closed',132000.0,'2026-04-19',68030,'2026-04-19','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-20/04/26-0088',18,'closed',38750.0,'2026-04-20',45563,'2026-04-20','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-21/04/26-0089',20,'closed',100600.0,'2026-04-21',116985,'2026-04-21','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-23/04/26-0090',21,'closed',22600.0,'2026-04-23',104024,'2026-04-23','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-24/04/26-0091',7,'closed',29600.0,'2026-04-24',15598,'2026-04-24','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-25/04/26-0092',19,'closed',19900.0,'2026-04-25',66610,'2026-04-25','Закрытый заказ','Great Wall Poer','А753ВС99');
INSERT INTO "orders" VALUES ('ZAK-26/04/26-0093',6,'closed',52150.0,'2026-04-26',38816,'2026-04-26','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-28/04/26-0094',5,'closed',45900.0,'2026-04-28',120352,'2026-04-28','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-29/04/26-0095',20,'closed',56100.0,'2026-04-29',21367,'2026-04-29','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-02/05/26-0096',13,'closed',201300.0,'2026-05-02',119049,'2026-05-02','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-03/05/26-0097',3,'closed',143600.0,'2026-05-03',34439,'2026-05-03','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-04/05/26-0098',12,'closed',41200.0,'2026-05-04',120870,'2026-05-04','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-05/05/26-0099',4,'closed',44800.0,'2026-05-05',101780,'2026-05-05','Закрытый заказ','Great Wall Poer','О159РТ40');
INSERT INTO "orders" VALUES ('ZAK-06/05/26-0100',18,'closed',256000.0,'2026-05-06',16882,'2026-05-06','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-07/05/26-0101',3,'closed',23100.0,'2026-05-07',47191,'2026-05-07','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-08/05/26-0102',10,'closed',38900.0,'2026-05-08',95035,'2026-05-08','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-09/05/26-0103',15,'closed',144400.0,'2026-05-09',121152,'2026-05-09','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-10/05/26-0104',10,'closed',26500.0,'2026-05-10',137567,'2026-05-10','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-11/05/26-0105',10,'closed',46412.0,'2026-05-11',96015,'2026-05-11','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-12/05/26-0106',12,'closed',31000.0,'2026-05-12',28894,'2026-05-12','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-13/05/26-0107',14,'closed',60500.0,'2026-05-13',85894,'2026-05-13','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-14/05/26-0108',4,'closed',28100.0,'2026-05-14',73783,'2026-05-14','Закрытый заказ','Great Wall Poer','О159РТ40');
INSERT INTO "orders" VALUES ('ZAK-15/05/26-0109',21,'closed',28000.0,'2026-05-15',39891,'2026-05-15','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-16/05/26-0110',5,'closed',176400.0,'2026-05-16',29753,'2026-05-16','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-17/05/26-0111',5,'closed',38500.0,'2026-05-17',125281,'2026-05-17','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-18/05/26-0112',15,'closed',30100.0,'2026-05-18',128466,'2026-05-18','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-19/05/26-0113',15,'closed',45100.0,'2026-05-19',20753,'2026-05-19','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-20/05/26-0114',17,'closed',80900.0,'2026-05-20',55235,'2026-05-20','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-21/05/26-0115',11,'closed',113250.0,'2026-05-21',23727,'2026-05-21','Закрытый заказ','Haval Dargo','К123МР77');
INSERT INTO "orders" VALUES ('ZAK-22/05/26-0116',3,'closed',37800.0,'2026-05-22',47074,'2026-05-22','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-23/05/26-0117',17,'closed',29200.0,'2026-05-23',34498,'2026-05-23','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-24/05/26-0118',5,'closed',156000.0,'2026-05-24',30228,'2026-05-24','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-25/05/26-0119',13,'closed',172600.0,'2026-05-25',50436,'2026-05-25','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-26/05/26-0120',10,'closed',35500.0,'2026-05-26',127683,'2026-05-26','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-27/05/26-0121',15,'closed',44600.0,'2026-05-27',131026,'2026-05-27','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-28/05/26-0122',21,'closed',29300.0,'2026-05-28',135983,'2026-05-28','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-29/05/26-0123',17,'closed',62100.0,'2026-05-29',48626,'2026-05-29','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-30/05/26-0124',3,'closed',86100.0,'2026-05-30',108281,'2026-05-30','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-31/05/26-0125',2,'closed',42100.0,'2026-05-31',85203,'2026-05-31','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-01/06/26-0126',5,'closed',58712.0,'2026-06-01',86355,'2026-06-01','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-02/06/26-0127',17,'closed',25900.0,'2026-06-02',143070,'2026-06-02','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-03/06/26-0128',12,'closed',44700.0,'2026-06-03',27710,'2026-06-03','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-04/06/26-0129',15,'closed',46200.0,'2026-06-04',135444,'2026-06-04','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-06/06/26-0130',17,'closed',30800.0,'2026-06-06',114392,'2026-06-06','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-08/06/26-0131',14,'closed',96700.0,'2026-06-08',114863,'2026-06-08','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-09/06/26-0132',14,'closed',167600.0,'2026-06-09',30690,'2026-06-09','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-10/06/26-0133',14,'closed',44300.0,'2026-06-10',53684,'2026-06-10','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-11/06/26-0134',13,'closed',55300.0,'2026-06-11',105239,'2026-06-11','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-14/06/26-0135',19,'closed',28600.0,'2026-06-14',18489,'2026-06-14','Закрытый заказ','Great Wall Poer','А753ВС99');
INSERT INTO "orders" VALUES ('ZAK-15/06/26-0136',11,'closed',21800.0,'2026-06-15',83056,'2026-06-15','Закрытый заказ','Haval Dargo','К123МР77');
INSERT INTO "orders" VALUES ('ZAK-17/06/26-0137',9,'closed',17100.0,'2026-06-17',126987,'2026-06-17','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-18/06/26-0138',15,'closed',55600.0,'2026-06-18',33013,'2026-06-18','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-19/06/26-0139',12,'closed',28900.0,'2026-06-19',143367,'2026-06-19','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-20/06/26-0140',19,'closed',47900.0,'2026-06-20',106707,'2026-06-20','Закрытый заказ','Great Wall Poer','А753ВС99');
INSERT INTO "orders" VALUES ('ZAK-21/06/26-0141',14,'closed',129600.0,'2026-06-21',117330,'2026-06-21','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-22/06/26-0142',18,'closed',19800.0,'2026-06-22',57700,'2026-06-22','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-23/06/26-0143',6,'closed',38500.0,'2026-06-23',94583,'2026-06-23','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-24/06/26-0144',21,'closed',133000.0,'2026-06-24',89383,'2026-06-24','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-25/06/26-0145',19,'closed',34600.0,'2026-06-25',20798,'2026-06-25','Закрытый заказ','Great Wall Poer','А753ВС99');
INSERT INTO "orders" VALUES ('ZAK-26/06/26-0146',16,'closed',18700.0,'2026-06-26',85935,'2026-06-26','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-27/06/26-0147',15,'closed',96900.0,'2026-06-27',63483,'2026-06-27','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-29/06/26-0148',15,'closed',26900.0,'2026-06-29',42118,'2026-06-29','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-30/06/26-0149',6,'closed',60900.0,'2026-06-30',95835,'2026-06-30','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-01/07/26-0150',7,'closed',25200.0,'2026-07-01',105033,'2026-07-01','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-02/07/26-0151',5,'closed',64800.0,'2026-07-02',35344,'2026-07-02','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-03/07/26-0152',2,'closed',52000.0,'2026-07-03',20201,'2026-07-03','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-04/07/26-0153',13,'closed',30400.0,'2026-07-04',88442,'2026-07-04','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-05/07/26-0154',4,'closed',56300.0,'2026-07-05',119193,'2026-07-05','Закрытый заказ','Great Wall Poer','О159РТ40');
INSERT INTO "orders" VALUES ('ZAK-06/07/26-0155',21,'closed',49700.0,'2026-07-06',109664,'2026-07-06','Закрытый заказ','Haval Jolion','А111АА63');
INSERT INTO "orders" VALUES ('ZAK-07/07/26-0156',14,'closed',22300.0,'2026-07-07',63448,'2026-07-07','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-09/07/26-0157',17,'closed',88900.0,'2026-07-09',108782,'2026-07-09','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "orders" VALUES ('ZAK-10/07/26-0158',9,'closed',40000.0,'2026-07-10',136924,'2026-07-10','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-11/07/26-0159',16,'closed',52600.0,'2026-07-11',93110,'2026-07-11','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-12/07/26-0160',5,'closed',29100.0,'2026-07-12',86140,'2026-07-12','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-13/07/26-0161',9,'closed',66700.0,'2026-07-13',81203,'2026-07-13','Закрытый заказ','Great Wall Poer','А159АМ64');
INSERT INTO "orders" VALUES ('ZAK-14/07/26-0162',2,'closed',127400.0,'2026-07-14',36266,'2026-07-14','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-16/07/26-0163',16,'closed',37600.0,'2026-07-16',62286,'2026-07-16','Закрытый заказ','Tank 500','В159А66');
INSERT INTO "orders" VALUES ('ZAK-18/07/26-0164',2,'closed',56500.0,'2026-07-18',55892,'2026-07-18','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-19/07/26-0165',4,'closed',18000.0,'2026-07-19',97829,'2026-07-19','Закрытый заказ','Great Wall Poer','О159РТ40');
INSERT INTO "orders" VALUES ('ZAK-20/07/26-0166',6,'closed',152600.0,'2026-07-20',130301,'2026-07-20','Закрытый заказ','Haval Dargo','К753ВМ01');
INSERT INTO "orders" VALUES ('ZAK-21/07/26-0167',14,'closed',95600.0,'2026-07-21',145077,'2026-07-21','Закрытый заказ','Great Wall Poer','У951КМ44');
INSERT INTO "orders" VALUES ('ZAK-22/07/26-0168',13,'closed',152300.0,'2026-07-22',71740,'2026-07-22','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-23/07/26-0169',4,'closed',34100.0,'2026-07-23',132000,'2026-07-23','Закрытый заказ','Great Wall Poer','О159РТ40');
INSERT INTO "orders" VALUES ('ZAK-24/07/26-0170',5,'closed',19500.0,'2026-07-24',135352,'2026-07-24','Закрытый заказ','Haval F7','У357АЕ99');
INSERT INTO "orders" VALUES ('ZAK-25/07/26-0171',20,'closed',29300.0,'2026-07-25',31461,'2026-07-25','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-26/07/26-0172',18,'closed',80100.0,'2026-07-26',49282,'2026-07-26','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-28/07/26-0173',15,'closed',38200.0,'2026-07-28',24584,'2026-07-28','Закрытый заказ','Haval F7','А753ЕМ55');
INSERT INTO "orders" VALUES ('ZAK-29/07/26-0174',13,'closed',79300.0,'2026-07-29',76704,'2026-07-29','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-30/07/26-0175',3,'closed',30700.0,'2026-07-30',133750,'2026-07-30','Закрытый заказ','Tank 300','М789НО50');
INSERT INTO "orders" VALUES ('ZAK-31/07/26-0176',12,'closed',23300.0,'2026-07-31',110264,'2026-07-31','Закрытый заказ','Tank 300','Х456РО99');
INSERT INTO "orders" VALUES ('ZAK-01/08/26-0177',8,'closed',23950.0,'2026-08-01',74883,'2026-08-01','Закрытый заказ','Haval Jolion','Т357ХО18');
INSERT INTO "orders" VALUES ('ZAK-02/08/26-0178',20,'closed',99600.0,'2026-08-02',14101,'2026-08-02','Закрытый заказ','Haval H9','А222АА77');
INSERT INTO "orders" VALUES ('ZAK-03/08/26-0179',2,'closed',13200.0,'2026-08-03',74155,'2026-08-03','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-04/08/26-0180',10,'closed',60000.0,'2026-08-04',71384,'2026-08-04','Закрытый заказ','Haval F7','В753НС31');
INSERT INTO "orders" VALUES ('ZAK-05/08/26-0181',2,'closed',31450.0,'2026-08-05',55910,'2026-08-05','Закрытый заказ','Haval Jolion','В456ЕК77');
INSERT INTO "orders" VALUES ('ZAK-06/08/26-0182',13,'closed',25700.0,'2026-08-06',58202,'2026-08-06','Закрытый заказ','Haval Jolion','Е789СН22');
INSERT INTO "orders" VALUES ('ZAK-07/08/26-0183',18,'closed',38600.0,'2026-08-07',148934,'2026-08-07','Закрытый заказ','Haval Jolion','М456КС88');
INSERT INTO "orders" VALUES ('ZAK-08/08/26-0184',7,'closed',54800.0,'2026-08-08',23510,'2026-08-08','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-09/08/26-0185',7,'closed',30000.0,'2026-08-09',135670,'2026-08-09','Закрытый заказ','Tank 500','Х951ТК22');
INSERT INTO "orders" VALUES ('ZAK-10/08/26-0186',8,'closed',27600.0,'2026-08-10',63611,'2026-08-10','Закрытый заказ','Haval Jolion','Т357ХО18');
INSERT INTO "orders" VALUES ('ZAK-11/08/26-0187',17,'closed',165550.0,'2026-08-11',140358,'2026-08-11','Закрытый заказ','Haval Dargo','С753РО77');
INSERT INTO "services" VALUES (1,'Аа7',12.0,12,'ASDFV-23-WE',0,0.0,0,'',0);
INSERT INTO "services" VALUES (2,'Балансировка колес',1000.0,40,'SRV-006',0,0.0,0,'',0);
INSERT INTO "services" VALUES (3,'Дезинфекция системы кондиционирования',2000.0,30,'GWM-AC-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (4,'Диагностика подвески',2500.0,30,'GWM-DIAG-05',0,0.0,0,'',0);
INSERT INTO "services" VALUES (5,'Диагностика тормозной системы',2500.0,30,'GWM-DIAG-06',0,0.0,0,'',0);
INSERT INTO "services" VALUES (6,'Диагностика ходовой',1200.0,45,'SRV-005',0,0.0,0,'',0);
INSERT INTO "services" VALUES (7,'Диагностика электрооборудования',3000.0,45,'GWM-ELEC-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (8,'Замена аккумулятора',1500.0,20,'GWM-ELEC-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (9,'Замена амортизаторов задних',8000.0,120,'GWM-SUS-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (10,'Замена амортизаторов передних',8000.0,120,'GWM-SUS-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (11,'Замена воздушного фильтра',800.0,30,'SRV-002',0,0.0,0,'',0);
INSERT INTO "services" VALUES (12,'Замена генератора',8000.0,120,'GWM-ELEC-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (13,'Замена гидрокомпенсаторов',80000.0,360,'GWM-ENG-06',0,0.0,0,'',0);
INSERT INTO "services" VALUES (14,'Замена главного тормозного цилиндра',7000.0,120,'GWM-BRK-07',0,0.0,0,'',0);
INSERT INTO "services" VALUES (15,'Замена задних тормозных дисков',5000.0,60,'GWM-BRK-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (16,'Замена задних тормозных колодок',2500.0,40,'GWM-BRK-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (17,'Замена компрессора кондиционера',15000.0,180,'GWM-AC-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (18,'Замена масла ДВС',1500.0,60,'SRV-001',0,0.0,0,'',0);
INSERT INTO "services" VALUES (19,'Замена масла в АКПП',7500.0,60,'GWM-TO-06',0,0.0,0,'',0);
INSERT INTO "services" VALUES (20,'Замена масла в ДВС',2500.0,30,'GWM-TO-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (21,'Замена масла в МКПП',5000.0,45,'GWM-TO-07',0,0.0,0,'',0);
INSERT INTO "services" VALUES (22,'Замена масла в раздаточной коробке',2000.0,30,'GWM-TO-09',0,0.0,0,'',0);
INSERT INTO "services" VALUES (23,'Замена масла в редукторе',2000.0,30,'GWM-TO-08',0,0.0,0,'',0);
INSERT INTO "services" VALUES (24,'Замена маслоотделителя',75000.0,240,'GWM-ENG-11',0,0.0,0,'',0);
INSERT INTO "services" VALUES (25,'Замена масляного насоса',6500.0,150,'GWM-ENG-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (26,'Замена масляного фильтра',800.0,15,'GWM-TO-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (27,'Замена моторного масла',2500.0,30,'GWM-TO-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (28,'Замена муфт изменения фаз ГРМ',60000.0,240,'GWM-ENG-12',0,0.0,0,'',0);
INSERT INTO "services" VALUES (29,'Замена опор двигателя',7500.0,120,'GWM-ENG-10',0,0.0,0,'',0);
INSERT INTO "services" VALUES (30,'Замена охлаждающей жидкости',1800.0,60,'SRV-007',0,0.0,0,'',0);
INSERT INTO "services" VALUES (31,'Замена охлаждающей жидкости (антифриз)',3500.0,60,'GWM-TO-11',0,0.0,0,'',0);
INSERT INTO "services" VALUES (32,'Замена передних тормозных дисков',5000.0,60,'GWM-BRK-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (33,'Замена передних тормозных колодок',2500.0,40,'GWM-BRK-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (34,'Замена помпы двигателя',10000.0,180,'GWM-ENG-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (35,'Замена поршневых колец',125000.0,480,'GWM-ENG-07',0,0.0,0,'',0);
INSERT INTO "services" VALUES (36,'Замена приводных ремней',2000.0,30,'GWM-TO-15',0,0.0,0,'',0);
INSERT INTO "services" VALUES (37,'Замена прокладки клапанной крышки',6000.0,120,'GWM-ENG-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (38,'Замена прокладки поддона',12500.0,180,'GWM-ENG-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (39,'Замена пружин подвески',6000.0,90,'GWM-SUS-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (40,'Замена радиатора кондиционера',10000.0,150,'GWM-AC-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (41,'Замена распредвала',75000.0,360,'GWM-ENG-08',0,0.0,0,'',0);
INSERT INTO "services" VALUES (42,'Замена ремня ГРМ',5500.0,180,'SRV-010',0,0.0,0,'',0);
INSERT INTO "services" VALUES (43,'Замена рулевых наконечников',4000.0,60,'GWM-SUS-06',0,0.0,0,'',0);
INSERT INTO "services" VALUES (44,'Замена сайлентблоков',5000.0,90,'GWM-SUS-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (45,'Замена салонного фильтра',1000.0,15,'GWM-TO-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (46,'Замена свечей зажигания',1600.0,45,'SRV-008',0,0.0,0,'',0);
INSERT INTO "services" VALUES (47,'Замена стартера',7000.0,120,'GWM-ELEC-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (48,'Замена суппорта',5000.0,90,'GWM-BRK-08',0,0.0,0,'',0);
INSERT INTO "services" VALUES (49,'Замена топливного фильтра',1500.0,30,'GWM-TO-05',0,0.0,0,'',0);
INSERT INTO "services" VALUES (50,'Замена тормозной жидкости',2500.0,45,'GWM-TO-10',0,0.0,0,'',0);
INSERT INTO "services" VALUES (51,'Замена тормозных колодок (зад)',2400.0,60,'SRV-004',0,0.0,0,'',0);
INSERT INTO "services" VALUES (52,'Замена тормозных колодок (перед)',3200.0,90,'SRV-003',0,0.0,0,'',0);
INSERT INTO "services" VALUES (53,'Замена тормозных шлангов',4000.0,60,'GWM-BRK-05',0,0.0,0,'',0);
INSERT INTO "services" VALUES (54,'Замена троса ручного тормоза',3500.0,60,'GWM-BRK-09',0,0.0,0,'',0);
INSERT INTO "services" VALUES (55,'Замена цепи ГРМ',60000.0,360,'GWM-TO-14',0,0.0,0,'',0);
INSERT INTO "services" VALUES (56,'Замена шаровых опор',4000.0,60,'GWM-SUS-05',0,0.0,0,'',0);
INSERT INTO "services" VALUES (57,'Замена щеток стеклоочистителя',500.0,10,'GWM-GEN-04',0,0.0,0,'',0);
INSERT INTO "services" VALUES (58,'Замер компрессии',4000.0,60,'GWM-DIAG-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (59,'Заправка кондиционера',3000.0,45,'GWM-AC-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (60,'Кап. ремонт двигателя',100000.0,720,'GWM-ENG-09',0,0.0,0,'',0);
INSERT INTO "services" VALUES (61,'Комплексная диагностика двигателя',5000.0,60,'GWM-DIAG-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (62,'Компьютерная диагностика',2000.0,30,'SRV-009',0,0.0,0,'',0);
INSERT INTO "services" VALUES (63,'Мойка двигателя',1500.0,30,'GWM-GEN-01',0,0.0,0,'',0);
INSERT INTO "services" VALUES (64,'Полировка кузова',8000.0,180,'GWM-GEN-03',0,0.0,0,'',0);
INSERT INTO "services" VALUES (65,'Прокачка тормозной системы',1500.0,30,'GWM-BRK-06',0,0.0,0,'',0);
INSERT INTO "services" VALUES (66,'Развал-схождение',2000.0,45,'GWM-SUS-07',0,0.0,0,'',0);
INSERT INTO "services" VALUES (67,'Регулировка ручного тормоза',1500.0,30,'GWM-BRK-10',0,0.0,0,'',0);
INSERT INTO "services" VALUES (68,'Устранение течи масла',5000.0,90,'GWM-ENG-05',0,0.0,0,'',0);
INSERT INTO "services" VALUES (69,'Химчистка салона',5000.0,120,'GWM-GEN-02',0,0.0,0,'',0);
INSERT INTO "services" VALUES (70,'Шиномонтаж (1 колесо)',800.0,20,'GWM-SUS-09',0,0.0,0,'',0);
INSERT INTO "services" VALUES (71,'Эндоскопия цилиндров',5000.0,60,'GWM-DIAG-04',0,0.0,0,'',0);
INSERT INTO "spare_parts" VALUES (1,'Аккумулятор 60Ah','BAT-60-01','Varta','Haval Jolion;Haval F7','',5500.0,8900.0,100.0,5.0,'Стеллаж Е1','шт');
INSERT INTO "spare_parts" VALUES (2,'Аккумулятор 75Ач Exide','GWM-ELC-001','Exide','Все модели GWM','',8500.0,13500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (3,'Амортизатор передний Dargo','GWM-SUS-004','GWM Original','Haval Jolion','',6800.0,11500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (4,'Антифриз G11','ANT-01','AGA','Haval Big Dog, Haval Dargo, Haval H6, Haval F7x, Haval Jolion, Haval F7, Haval H9','',1200.0,1500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (5,'Воздушный фильтр GWM','GWM-FLT-002','GWM Original','Все модели GWM','',450.0,800.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (6,'Диск тормозной задний','3502108XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,5500.0,100.0,5.0,'Склад А','шт');
INSERT INTO "spare_parts" VALUES (7,'Диск тормозной передний','3501104XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,6500.0,100.0,5.0,'Склад А','шт');
INSERT INTO "spare_parts" VALUES (8,'Диск тормозной передний Dargo','GWM-BRK-003','GWM Original','Haval Dargo','',3800.0,6200.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (9,'Жидкость для омывателя','GWM-WASH-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',500.0,600.0,100.0,5.0,'Склад Д','шт');
INSERT INTO "spare_parts" VALUES (10,'Жидкость тормозная DOT 4','GWM-DOT4-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',300.0,400.0,100.0,5.0,'Склад Д','шт');
INSERT INTO "spare_parts" VALUES (11,'Зеркало заднего вида левое','8210100XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,8500.0,100.0,5.0,'Склад З','шт');
INSERT INTO "spare_parts" VALUES (12,'Зеркало заднего вида правое','8210100XPW01B','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,8500.0,100.0,5.0,'Склад З','шт');
INSERT INTO "spare_parts" VALUES (13,'Катушка зажигания GWM','GWM-ENG-004','GWM Original','Все модели GWM','',2500.0,4500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (14,'Колодки тормозные задние','3502139XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,3500.0,100.0,5.0,'Склад А','шт');
INSERT INTO "spare_parts" VALUES (15,'Колодки тормозные задние GWM','GWM-BRK-002','GWM Original','Haval F7, Haval Dargo','',2200.0,3800.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (16,'Колодки тормозные передние (комплект)','3501119XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,4000.0,100.0,5.0,'Склад А','шт');
INSERT INTO "spare_parts" VALUES (17,'Колодки тормозные передние GWM','GWM-BRK-001','GWM Original','Haval Dargo, Haval F7','',2500.0,4200.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (18,'Масло моторное 0W-30','0W30-ZIC','ZIC','','',500.0,1000.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (19,'Масло моторное 5W-30','GWM-5W30-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',1000.0,1200.0,100.0,5.0,'Склад Д','шт');
INSERT INTO "spare_parts" VALUES (20,'Масло трансмиссионное ATF','GWM-ATF-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',1200.0,1500.0,100.0,5.0,'Склад Д','шт');
INSERT INTO "spare_parts" VALUES (21,'Масляный фильтр GWM','GWM-FLT-001','GWM Original','Все модели GWM','',350.0,650.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (22,'Моторное масло 5W-30 ZIC','ZICOIL-5W30','ZIC','Haval Jolion;Haval F7;Haval F5','',2200.0,3200.0,100.0,5.0,'Стеллаж А1','шт');
INSERT INTO "spare_parts" VALUES (23,'Охлаждающая жидкость','GWM-ANT-005','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',1000.0,2200.0,100.0,5.0,'Склад Д','шт');
INSERT INTO "spare_parts" VALUES (24,'Поворотник левый','4115100XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,3500.0,100.0,5.0,'Склад Ж','шт');
INSERT INTO "spare_parts" VALUES (25,'Поворотник правый','4115100XPW01B','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,3500.0,100.0,5.0,'Склад Ж','шт');
INSERT INTO "spare_parts" VALUES (26,'Пробка сливная картера двигателя','1009304XEC06','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,200.0,100.0,5.0,'Склад В','шт');
INSERT INTO "spare_parts" VALUES (27,'Прокладка сливной пробки картера','1009305XEC06','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,100.0,100.0,5.0,'Склад В','шт');
INSERT INTO "spare_parts" VALUES (28,'Ремень ГРМ','GWM-BELT-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,3500.0,100.0,5.0,'Склад Е','шт');
INSERT INTO "spare_parts" VALUES (29,'Ремень ГРМ комплект Dargo','GWM-ENG-001','Gates','Haval Dargo, Tank 300','',3800.0,6200.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (30,'Ремень генератора GWM','GWM-ENG-002','Contitech','Все модели GWM','',850.0,1500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (31,'Ремень поликлиновой','GWM-BELT-002','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,2500.0,100.0,5.0,'Склад Е','шт');
INSERT INTO "spare_parts" VALUES (32,'Ролик направляющий','GWM-GUI-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,2200.0,100.0,5.0,'Склад Е','шт');
INSERT INTO "spare_parts" VALUES (33,'Ролик натяжной ремня','GWM-TEN-001','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,2800.0,100.0,5.0,'Склад Е','шт');
INSERT INTO "spare_parts" VALUES (34,'Салонный фильтр GWM','GWM-FLT-003','GWM Original','Все модели GWM','',380.0,700.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (35,'Свеча зажигания','3707100WEC05','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,600.0,100.0,5.0,'Склад В','шт');
INSERT INTO "spare_parts" VALUES (36,'Свеча накала (дизель)','3707100WEC06','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,1200.0,100.0,5.0,'Склад В','шт');
INSERT INTO "spare_parts" VALUES (37,'Свечи зажигания NGK 4шт','GWM-ENG-003','NGK','Все модели GWM','',1800.0,3200.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (38,'Стекло боковое левое','6202100XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,12000.0,100.0,5.0,'Склад З','шт');
INSERT INTO "spare_parts" VALUES (39,'Стекло боковое правое','6202100XPW01B','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,12000.0,100.0,5.0,'Склад З','шт');
INSERT INTO "spare_parts" VALUES (40,'Стекло лобовое','6201100XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,25000.0,100.0,5.0,'Склад З','шт');
INSERT INTO "spare_parts" VALUES (41,'Термостат GWM','GWM-ENG-005','GWM Original','Все модели GWM','',1200.0,2200.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (42,'Топливный фильтр','FLT-FUEL-01','MANN','Haval F7;Haval Dargo','',700.0,1200.0,100.0,5.0,'Стеллаж А3','шт');
INSERT INTO "spare_parts" VALUES (43,'Фара передняя левая','4112100XPW01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,18000.0,100.0,5.0,'Склад Ж','шт');
INSERT INTO "spare_parts" VALUES (44,'Фара передняя правая','4112100XPW01B','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,18000.0,100.0,5.0,'Склад Ж','шт');
INSERT INTO "spare_parts" VALUES (45,'Фильтр воздушный','1109103XKM01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,1200.0,100.0,5.0,'Склад Б','шт');
INSERT INTO "spare_parts" VALUES (46,'Фильтр масляный','','','','',3000.0,5000.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (47,'Фильтр масляный Haval','FLT-ENG-01','MANN','Haval Jolion;Haval F7','',400.0,650.0,100.0,5.0,'Стеллаж А2','шт');
INSERT INTO "spare_parts" VALUES (48,'Фильтр салона (пылевой)','8104300XKR02A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,1500.0,100.0,5.0,'Склад Б','шт');
INSERT INTO "spare_parts" VALUES (49,'Фильтр топливный','1117100XKW09A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,1800.0,100.0,5.0,'Склад Б','шт');
INSERT INTO "spare_parts" VALUES (50,'Шаровая опора передняя Dargo','GWM-SUS-001','GWM Original','Haval Dargo, Tank 300','',3200.0,5500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (51,'Шаровая опора усиленная Tank','TNK-SUS-001','GWM Original','Tank 300','',4500.0,7500.0,100.0,5.0,'','шт');
INSERT INTO "spare_parts" VALUES (52,'Щётка двери багажника','6310105XKM01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,900.0,100.0,5.0,'Склад Г','шт');
INSERT INTO "spare_parts" VALUES (53,'Щётка лобового стекла левая','5205102XKM01A','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,1200.0,100.0,5.0,'Склад Г','шт');
INSERT INTO "spare_parts" VALUES (54,'Щётка лобового стекла правая','5205105XPW01B','GWM','Tank 300 (2.0 AT 4WD, 2023-2026)','',0.0,1200.0,100.0,5.0,'Склад Г','шт');
INSERT INTO "spare_parts" VALUES (55,'Щётки стеклоочистителя Bosch','GWM-ELC-005','Bosch','Все модели GWM','',1200.0,2200.0,100.0,5.0,'','шт');
CREATE INDEX IF NOT EXISTS "idx_appointments_client_id" ON "appointments" (
	"client_id"
);
CREATE INDEX IF NOT EXISTS "idx_appointments_date" ON "appointments" (
	"appointment_date"
);
CREATE INDEX IF NOT EXISTS "idx_appointments_service_id" ON "appointments" (
	"service_id"
);
CREATE INDEX IF NOT EXISTS "idx_appointments_status" ON "appointments" (
	"status"
);
CREATE INDEX IF NOT EXISTS "idx_client_cars_client_id" ON "client_cars" (
	"client_id"
);
CREATE INDEX IF NOT EXISTS "idx_clients_name_lastname_phone" ON "clients" (
	"name",
	"last_name",
	"phone"
);
CREATE INDEX IF NOT EXISTS "idx_order_parts_order_id" ON "order_parts" (
	"order_id"
);
CREATE INDEX IF NOT EXISTS "idx_order_parts_spare_part_id" ON "order_parts" (
	"spare_part_id"
);
CREATE INDEX IF NOT EXISTS "idx_order_services_order_id" ON "order_services" (
	"order_id"
);
CREATE INDEX IF NOT EXISTS "idx_order_services_service_id" ON "order_services" (
	"service_id"
);
CREATE INDEX IF NOT EXISTS "idx_orders_client_id" ON "orders" (
	"client_id"
);
CREATE INDEX IF NOT EXISTS "idx_orders_created_date" ON "orders" (
	"created_date"
);
CREATE INDEX IF NOT EXISTS "idx_orders_status" ON "orders" (
	"status"
);
CREATE INDEX IF NOT EXISTS "idx_service_parts_service_id" ON "service_parts" (
	"service_id"
);
CREATE INDEX IF NOT EXISTS "idx_service_parts_spare_part_id" ON "service_parts" (
	"spare_part_id"
);
CREATE INDEX IF NOT EXISTS "idx_service_spare_parts_list_items_list_id" ON "service_spare_parts_list_items" (
	"list_id"
);
CREATE INDEX IF NOT EXISTS "idx_service_spare_parts_list_items_spare_part_id" ON "service_spare_parts_list_items" (
	"spare_part_id"
);
CREATE INDEX IF NOT EXISTS "idx_service_spare_parts_lists_service_id" ON "service_spare_parts_lists" (
	"service_id"
);
CREATE INDEX IF NOT EXISTS "idx_service_spare_parts_service_id" ON "service_spare_parts" (
	"service_id"
);
CREATE INDEX IF NOT EXISTS "idx_service_spare_parts_spare_part_id" ON "service_spare_parts" (
	"spare_part_id"
);
CREATE INDEX IF NOT EXISTS "idx_spare_parts_name" ON "spare_parts" (
	"name"
);
CREATE INDEX IF NOT EXISTS "idx_to_parts_car_model" ON "to_parts" (
	"car_model"
);
CREATE INDEX IF NOT EXISTS "idx_to_parts_spare_part_id" ON "to_parts" (
	"spare_part_id"
);
COMMIT;
