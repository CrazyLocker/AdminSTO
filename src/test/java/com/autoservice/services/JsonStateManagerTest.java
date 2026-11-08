package com.autoservice.services;

import com.autoservice.TestTags;
import com.autoservice.model.WindowState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты общего JSON-хранилища состояний.
 */
@Tag(TestTags.UNIT)
class JsonStateManagerTest {

    @TempDir
    Path storageDir;

    @Test
    void saveWritesJsonFileAndCachesValue() {
        TestStateManager manager = new TestStateManager(storageDir);

        manager.save("alpha", "C:/path");

        assertThat(storageDir.resolve("alpha.json")).exists();
        assertThat(manager.get("alpha")).isEqualTo("C:/path");
    }

    @Test
    void newManagerLoadsValuesFromStorage() throws IOException {
        TestStateManager first = new TestStateManager(storageDir);
        first.save("beta", "value");

        TestStateManager second = new TestStateManager(storageDir);

        assertThat(second.get("beta")).isEqualTo("value");
        assertThat(Files.readString(storageDir.resolve("beta.json"), StandardCharsets.UTF_8))
                .contains("value");
    }

    @Test
    void getLoadsValueCreatedAfterStartup() throws IOException {
        TestStateManager manager = new TestStateManager(storageDir);
        Files.writeString(storageDir.resolve("late.json"), "\"late-value\"", StandardCharsets.UTF_8);

        assertThat(manager.get("late")).isEqualTo("late-value");
    }

    @Test
    void resetRemovesFileAndCachedValue() {
        TestStateManager manager = new TestStateManager(storageDir);
        manager.save("gamma", "value");

        manager.reset("gamma");

        assertThat(storageDir.resolve("gamma.json")).doesNotExist();
        assertThat(manager.get("gamma")).isNull();
    }

    @Test
    void resetDropsCachedValueWhenFileWasRemovedExternally() throws IOException {
        TestStateManager manager = new TestStateManager(storageDir);
        manager.save("delta", "value");
        Files.delete(storageDir.resolve("delta.json"));

        manager.reset("delta");

        assertThat(manager.get("delta")).isNull();
    }

    @Test
    void saveSkipsNullValues() {
        TestStateManager manager = new TestStateManager(storageDir);

        manager.save("epsilon", null);

        assertThat(storageDir.resolve("epsilon.json")).doesNotExist();
        assertThat(manager.get("epsilon")).isNull();
    }

    @Test
    void invalidKeysAreIgnored() {
        TestStateManager manager = new TestStateManager(storageDir);

        assertThatCode(() -> {
            manager.save(null, "value");
            manager.save("  ", "value");
            manager.get(null);
            manager.reset("");
        }).doesNotThrowAnyException();

        assertThat(storageDir).isEmptyDirectory();
    }

    @Test
    void storesTypedValues() {
        WindowStateTestManager manager = new WindowStateTestManager(storageDir);
        manager.save("main", new WindowState(10, 20, 800, 600));

        WindowStateTestManager reloaded = new WindowStateTestManager(storageDir);
        WindowState state = reloaded.get("main");

        assertThat(state).isNotNull();
        assertThat(state.getX()).isEqualTo(10);
        assertThat(state.getY()).isEqualTo(20);
        assertThat(state.getWidth()).isEqualTo(800);
        assertThat(state.getHeight()).isEqualTo(600);
    }

    private static class TestStateManager extends JsonStateManager<String> {
        TestStateManager(Path storageDir) {
            super(storageDir.toString(), String.class);
        }
    }

    private static class WindowStateTestManager extends JsonStateManager<WindowState> {
        WindowStateTestManager(Path storageDir) {
            super(storageDir.toString(), WindowState.class);
        }
    }
}
