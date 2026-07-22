#!/bin/bash
# ============================================================
# Сборка полностью автономной portable-версии AdminSTO
# Работает на Linux (Ubuntu) в CI/CD
# ============================================================

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$PROJECT_DIR/AdminSTO_Portable"
JAR_FILE="$PROJECT_DIR/build/libs/autoservice-admin.jar"
JAVAFX_VERSION="21.0.6"

echo "=================================================="
echo "  AdminSTO Portable Build (Linux/CI)"
echo "=================================================="

# Проверка fat JAR
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: fat JAR не найден: $JAR_FILE"
    echo "Запустите: ./gradlew clean fatJar"
    exit 1
fi

# Очистка
echo "[1/5] Подготовка..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/lib"
mkdir -p "$DIST_DIR/native"
mkdir -p "$DIST_DIR/data"
mkdir -p "$DIST_DIR/logs"
mkdir -p "$DIST_DIR/backups"
mkdir -p "$DIST_DIR/config/table-state"
mkdir -p "$DIST_DIR/config/window-state"
echo "  Готово."

# Создание JRE через jlink
echo "[2/5] Создание JRE (jlink)..."
JDK_HOME="${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}"
echo "  JDK: $JDK_HOME"

"$JDK_HOME/bin/jlink" \
    --module-path "$JDK_HOME/jmods" \
    --add-modules java.base,java.sql,java.logging,java.instrument,java.management,jdk.zipfs,java.xml,jdk.httpserver,jdk.unsupported,jdk.localedata,java.naming,java.scripting,java.desktop \
    --include-locales ru \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output "$DIST_DIR/jre"

JRE_SIZE=$(du -sh "$DIST_DIR/jre" | cut -f1)
echo "  Готово. (~$JRE_SIZE)"

# Копирование JavaFX JAR (из gradle cache)
echo "[3/5] Копирование JavaFX..."
GRADLE_CACHE="$HOME/.gradle/caches/modules-2/files-2.1/org.openjfx"

for mod in javafx-controls javafx-fxml javafx-base javafx-graphics; do
    # Ищем JAR для JavaFX 21.0.6 (без -win, без -sources, без -javadoc)
    JAR=$(find "$GRADLE_CACHE/$mod" -name "*.jar" -path "*/21.0.6/*" ! -name "*-win.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" -type f | head -1)
    if [ -z "$JAR" ]; then
        echo "  ERROR: $mod 21.0.6 не найден в gradle cache!"
        echo "  Запустите: ./gradlew dependencies"
        exit 1
    fi
    cp "$JAR" "$DIST_DIR/lib/${mod}.jar"
    echo "  + ${mod}.jar"
done

# Копирование ресурсов приложения
echo "[4/5] Копирование ресурсов..."
cp "$JAR_FILE" "$DIST_DIR/autoservice-admin.jar"

if [ -f "$PROJECT_DIR/src/main/resources/styles.css" ]; then
    cp "$PROJECT_DIR/src/main/resources/styles.css" "$DIST_DIR/styles.css"
fi

if [ -f "$PROJECT_DIR/src/main/resources/logback.xml" ]; then
    cp "$PROJECT_DIR/src/main/resources/logback.xml" "$DIST_DIR/logback.xml"
fi

# Копирование config
if [ -d "$PROJECT_DIR/config/table-state" ]; then
    cp -r "$PROJECT_DIR/config/table-state"/* "$DIST_DIR/config/table-state/" 2>/dev/null || true
fi

if [ -d "$PROJECT_DIR/config/window-state" ]; then
    cp -r "$PROJECT_DIR/config/window-state"/* "$DIST_DIR/config/window-state/" 2>/dev/null || true
fi

echo "  Готово."

# Создание launcher script
echo "[5/5] Создание launcher.sh..."
cat > "$DIST_DIR/run.sh" << 'EOF'
#!/bin/bash
set -e
cd "$(dirname "$0")"

if [ ! -f "jre/bin/java" ]; then
    echo "ERROR: Java не найдена!"
    exit 1
fi

if [ ! -f "lib/javafx-controls.jar" ]; then
    echo "ERROR: JavaFX не найдена!"
    exit 1
fi

echo "============================================"
echo " AdminSTO - Administrator STO"
echo "============================================"
echo ""
echo "Запуск..."
echo ""

jre/bin/java \
    -Duser.language=ru \
    -Duser.country=RU \
    -Dfile.encoding=UTF-8 \
    -Dapp.home="$(pwd)" \
    --module-path "lib" \
    --add-modules javafx.controls,javafx.fxml \
    --add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED \
    --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED \
    --add-opens javafx.graphics/javafx.scene.effect=ALL-UNNAMED \
    --add-opens javafx.graphics/javafx.scene.shape=ALL-UNNAMED \
    --add-opens javafx.base/com.sun.javafx.event=ALL-UNNAMED \
    --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED \
    --add-opens javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED \
    --add-opens javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED \
    --add-opens javafx.base/com.sun.javafx.binding=ALL-UNNAMED \
    --add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED \
    --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED \
    -cp "autoservice-admin.jar;styles.css" \
    com.autoservice.Launcher

if [ $? -ne 0 ]; then
    echo ""
    echo "============================================"
    echo "  ОШИБКА запуска!"
    echo "============================================"
    echo ""
    echo "Проверьте:"
    echo "  1. Вся папка скопирована целиком?"
    echo "  2. Все зависимости установлены?"
    echo ""
    exit 1
fi
EOF

chmod +x "$DIST_DIR/run.sh"

# Итог
TOTAL_SIZE=$(du -sh "$DIST_DIR" | cut -f1)
echo ""
echo "=================================================="
echo "  Portable готово! Размер: $TOTAL_SIZE"
echo "=================================================="
echo "Папка: $DIST_DIR"
echo "Запуск: ./run.sh"
