package com.autoservice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер «не мутировать»: класс или метод с этой аннотацией PIT пропускает.
 *
 * <p>Нужен для случаев, которые не ловятся ни одним шаблоном имён в
 * {@code pitest {}} (например, отдельный метод со сложной ветвью, которую
 * бессмысленно тестировать из-за внешней зависимости). Сама аннотация
 * распознаётся PIT без дополнительной настройки сборки.</p>
 *
 * <p>Использование:</p>
 * <pre>{@code
 * @DoNotMutate
 * private void showSystemTrayNotification() { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DoNotMutate {
}
