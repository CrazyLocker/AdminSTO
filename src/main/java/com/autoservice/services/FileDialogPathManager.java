package com.autoservice.services;

/**
 * Управление путями для импорт/экспорт файлов: сохранение и восстановление последнего каталога.
 */
public class FileDialogPathManager extends JsonStateManager<String> {
    private static final String STORAGE_DIR = "config/file-paths";

    private static FileDialogPathManager instance;

    private FileDialogPathManager() {
        super(STORAGE_DIR, String.class);
    }

    /**
     * Получить singleton-экземпляр менеджера.
     * @return экземпляр менеджера
     */
    public static synchronized FileDialogPathManager getInstance() {
        if (instance == null) {
            instance = new FileDialogPathManager();
        }
        return instance;
    }

    /**
     * Сохранить последний путь для конкретного диалога.
     * @param dialogId идентификатор диалога
     * @param path последний выбранный путь
     */
    public void saveLastPath(String dialogId, String path) {
        if (path == null || path.trim().isEmpty()) {
            logger.debug("Empty path for {}, skipping save", dialogId);
            return;
        }

        save(dialogId, path);
    }

    /**
     * Получить последний сохранённый путь для конкретного диалога.
     * @param dialogId идентификатор диалога
     * @return последний путь или {@code null}, если он не найден
     */
    public String getLastPath(String dialogId) {
        String path = get(dialogId);
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return path;
    }

    /**
     * Удалить сохранённый путь для конкретного диалога.
     * @param dialogId идентификатор диалога
     */
    public void resetLastPath(String dialogId) {
        reset(dialogId);
    }
}
