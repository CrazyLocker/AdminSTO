package com.autoservice.views;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DatabaseFactory;
import com.autoservice.TestTags;
import com.autoservice.support.JavaFxTestSupport;
import com.autoservice.utils.LoggerManager;
import com.autoservice.utils.ThemeManager;
import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableRow;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI-тест оформления таблиц: чередование строк («зебра») и голубая подсветка
 * выбранной строки.
 *
 * <p>Проверяет не наличие CSS-правила в файле, а фактический цвет фона,
 * рассчитанный JavaFX для строк таблицы, — так тест поймает и поломку
 * подключения стилей к сцене, и перекрытие правил темой.</p>
 */
@Tag(TestTags.UI)
class TableStripingUITest {

    private Stage stage;

    @BeforeAll
    static void setUpClass() {
        JavaFxTestSupport.initToolkit();
        LoggerManager.init();
        ThemeManager.applySavedTheme();
        DatabaseFactory.initForTest();
        DataStore.load();
        seed();
    }

    @AfterAll
    static void tearDownClass() {
        DatabaseFactory.close();
    }

    @AfterEach
    void closeStage() throws InterruptedException {
        if (stage != null) {
            Stage current = stage;
            JavaFxTestSupport.runOnFxThread(current::close);
            stage = null;
        }
    }

    @Test
    void rowsAlternateTheirBackground() throws InterruptedException {
        List<Color> backgrounds = showClientViewAndReadRowBackgrounds();

        assertThat(backgrounds)
                .as("в таблице клиентов четыре строки данных")
                .hasSize(4);

        Color first = backgrounds.get(0);
        Color second = backgrounds.get(1);
        Color third = backgrounds.get(2);

        assertThat(lightness(first))
                .as("чётные строки остаются на светлом фоне по умолчанию")
                .isGreaterThan(0.95);
        assertThat(lightness(second))
                .as("нечётная строка залита серым — заметно темнее обычной")
                .isLessThan(lightness(first) - 0.03);
        assertThat(third)
                .as("чередование повторяется: третья строка как первая")
                .isEqualTo(first);
    }

    @Test
    void rowsKeepAlternatingInDarkTheme() throws InterruptedException {
        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        try {
            List<Color> backgrounds = showClientViewAndReadRowBackgrounds();

            assertThat(backgrounds).hasSize(4);
            assertThat(backgrounds.get(1))
                    .as("в тёмной теме соседние строки тоже различаются фоном")
                    .isNotEqualTo(backgrounds.get(0));
            assertThat(backgrounds.get(2))
                    .as("в тёмной теме чередование повторяется")
                    .isEqualTo(backgrounds.get(0));
        } finally {
            ThemeManager.applySavedTheme();
        }
    }

    @Test
    void selectedRowIsHighlightedWithAccentColor() throws InterruptedException {
        Scene scene = showClientView();

        List<TableRow<?>> rows = rowsOf(scene);
        TableRow<?> target = rows.get(0);
        Color normal = backgroundOf(target);

        JavaFxTestSupport.runOnFxThread(() -> {
            target.getTableView().getSelectionModel().select(target.getIndex());
            target.getTableView().requestFocus();
            target.getTableView().applyCss();
            target.getTableView().layout();
        });

        Color selected = backgroundOf(target);

        assertThat(selected)
                .as("выбранная строка окрашивается иначе, чем обычная")
                .isNotEqualTo(normal);
        assertThat(selected.getBlue())
                .as("подсветка выбора — голубая (-color-accent-subtle): синий канал не меньше красного")
                .isGreaterThanOrEqualTo(selected.getRed());
    }

    /**
     * Воспринимаемая светлота цвета (среднее по каналам).
     */
    private static double lightness(Color color) {
        return (color.getRed() + color.getGreen() + color.getBlue()) / 3;
    }

    // ====== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ======

    private List<Color> showClientViewAndReadRowBackgrounds() throws InterruptedException {
        Scene scene = showClientView();
        List<TableRow<?>> rows = rowsOf(scene);
        List<Color> backgrounds = new ArrayList<>();
        for (TableRow<?> row : rows) {
            backgrounds.add(backgroundOf(row));
        }
        return backgrounds;
    }

    private Scene showClientView() throws InterruptedException {
        Region view = JavaFxTestSupport.callOnFxThread(ClientView::create);

        JavaFxTestSupport.runOnFxThread(() -> {
            Scene scene = new Scene(view, 1200, 700);
            ThemeManager.applySceneStyles(scene);
            stage = new Stage();
            stage.setScene(scene);
            stage.show();
            view.applyCss();
            view.layout();
        });

        Scene scene = JavaFxTestSupport.callOnFxThread(stage::getScene);
        assertThat(scene.getStylesheets())
                .as("к сцене подключены общие стили приложения")
                .anyMatch(stylesheet -> stylesheet.endsWith("global-custom.css"));
        return scene;
    }

    private List<TableRow<?>> rowsOf(Scene scene) throws InterruptedException {
        List<TableRow<?>> rows = JavaFxTestSupport.callOnFxThread(() -> scene.getRoot()
                .lookupAll(".table-row-cell").stream()
                .filter(node -> node instanceof TableRow)
                .map(node -> (TableRow<?>) node)
                .filter(row -> !row.isEmpty() && row.getIndex() >= 0)
                .sorted(Comparator.comparingInt(row -> ((TableRow<?>) row).getIndex()))
                .collect(Collectors.toList()));
        return rows;
    }

    private Color backgroundOf(TableRow<?> row) throws InterruptedException {
        return JavaFxTestSupport.callOnFxThread(() -> {
            // Тема задаёт фон строки несколькими слоями (линия-разделитель,
            // базовый фон, полупрозрачная заливка «зебры»). Накладываем их
            // последовательно — так получается цвет, который видит глаз.
            List<BackgroundFill> fills = row.getBackground() == null
                    ? List.of()
                    : row.getBackground().getFills();
            Color result = Color.WHITE;
            for (BackgroundFill fill : fills) {
                Paint paint = fill.getFill();
                if (paint instanceof Color) {
                    result = blend(result, (Color) paint);
                }
            }
            return result;
        });
    }

    /**
     * Наложение полупрозрачного слоя {@code top} поверх {@code bottom}.
     */
    private static Color blend(Color bottom, Color top) {
        double alpha = top.getOpacity();
        if (alpha >= 1.0) {
            return top;
        }
        double red = top.getRed() * alpha + bottom.getRed() * (1 - alpha);
        double green = top.getGreen() * alpha + bottom.getGreen() * (1 - alpha);
        double blue = top.getBlue() * alpha + bottom.getBlue() * (1 - alpha);
        return new Color(red, green, blue, 1.0);
    }

    private static void seed() {
        DataStore.addClient(new Client("Алексей", "Кузнецов", "+7 900 111-00-01", "Toyota Camry", "А123ВС163"));
        DataStore.addClient(new Client("Анна", "Морозова", "+7 900 111-00-02", "Hyundai Creta", "В456ЕК163"));
        DataStore.addClient(new Client("Олег", "Белов", "+7 900 111-00-03", "Mercedes C200", "С789МН163"));
        DataStore.addClient(new Client("Ирина", "Смирнова", "+7 900 111-00-04", "Renault Logan", "Е321ОР163"));
    }
}
