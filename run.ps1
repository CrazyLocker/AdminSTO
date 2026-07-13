cd "E:\Alecsey\Java Projects for QA\AdminSTO"

$libPath = "E:\Alecsey\Java Projects for QA\AdminSTO\portable\lib"
$jarFiles = Get-ChildItem -Path $libPath -Filter "*.jar" | ForEach-Object { $_.FullName }
$classpath = $jarFiles -join ";"

# JavaFX 17+ требует указания module-path и add-modules
java `
    --module-path "$libPath" `
    --add-modules javafx.controls,javafx.fxml `
    -cp "target/classes;$classpath" `
    com.autoservice.App
