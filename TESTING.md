# Тестирование проекта AdminSTO

## Архитектура тестов

Проект использует следующую структуру:

```
src/test/java/com/autoservice/
├── BaseTest.java              # Базовый класс для всех тестов
├── TestTags.java              # Теги для классификации тестов
├── builders/                  # Builder-классы для создания тестовых данных
│   ├── ClientBuilder.java
│   ├── ServiceBuilder.java
│   ├── SparePartBuilder.java
│   ├── WorkOrderBuilder.java
│   └── AppointmentBuilder.java
├── dialogs/                   # Тесты диалогов
│   ├── EditClientDialogTest.java
│   ├── CreateOrderDialogTest.java
│   ├── EditOrderDialogTest.java
│   └── ImportSparePartsDialogTest.java
├── controllers/               # Тесты контроллеров
│   ├── ClientControllerTest.java
│   ├── OrderControllerTest.java
│   └── DictionaryControllerTest.java
├── services/                  # Тесты сервисов
│   └── ImportServiceTest.java
└── [Model]Test.java           # Тесты моделей
    ├── ClientTest.java
    ├── ServiceTest.java
    ├── SparePartTest.java
    ├── AppointmentTest.java
    └── WorkOrderTest.java
```

## Как запускать тесты

### Запуск всех тестов
```bash
mvn test
```

### Запуск тестов с отчётом JaCoCo
```bash
mvn test jacoco:report
```

Отчёт будет создан в `target/site/jacoco/`

### Запуск только unit-тестов
```bash
mvn test -Dgroups=unit
```

### Запуск только интеграционных тестов
```bash
mvn test -Dgroups=integration
```

### Запуск определённого тестового класса
```bash
mvn test -Dtest=ClientTest
mvn test -Dtest=ClientControllerTest
mvn test -Dtest="*DialogTest"
```

## Правила именования тестов

### Общий формат
```
test[MethodName][Condition][Result]
```

### Примеры
- `testAddClient_validData_success`
- `testUpdateClient_notFound_exception`
- `testValidatePhone_empty_returnsFalse`
- `testCalculateTotal_withServicesAndParts_correctAmount`

## Список тегов

| Тег | Описание |
|-----|----------|
| `unit` | Быстрые изолированные тесты |
| `integration` | Тесты с БД, сетью, внешними системами |
| `slow` | Долгие тесты (>1 сек) |
| `ui` | Тесты UI/диалогов |
| `controller` | Тесты контроллеров |
| `service` | Тесты сервисов |

## Как добавлять новые тесты

1. **Выберите класс** `BaseTest` для наследования
2. **Используйте теги** для классификации теста
3. **Создайте тестовый метод** с описательным именем
4. **Используйте builders** для создания тестовых данных
5. **Следуйте правилам именования**

### Пример нового теста
```java
package com.autoservice;

import com.autoservice.builders.ClientBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag(TestTags.UNIT)
class NewFeatureTest extends BaseTest {

    @Test
    void testNewFeature_withValidInput_returnsExpected() {
        // Given
        Client client = new ClientBuilder()
            .withName("Ivan")
            .withLastName("Petrov")
            .withPhone("+79991234567")
            .build();

        // When
        var result = SomeClass.newFeature(client);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Ivan Petrov");
    }
}
```

## Ожидаемые показатели качества

- **Покрытие кода**: ≥85%
- **Время выполнения всех тестов**: <10 сек
- **Количество flaky-тестов**: 0
- **Количество тестов**: ≥700
