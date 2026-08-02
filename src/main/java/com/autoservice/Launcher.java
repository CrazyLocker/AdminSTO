package com.autoservice;

/**
 * Launcher for fat-jar execution.
 *
 * <p>App extends javafx.application.Application, so when App is the Main-Class,
 * the JVM checks for JavaFX runtime on the module path before main() is even
 * called. In a fat jar JavaFX lives on the classpath (unnamed module), not the
 * module path, so that check fails with "JavaFX runtime components are missing".
 *
 * <p>This class does not extend Application, bypassing the check. It then
 * delegates to App.launch() which loads JavaFX from the classpath normally.
 */
public class Launcher {

    public static void main(String[] args) {
        App.launch(App.class, args);
    }
}
