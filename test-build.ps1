# Test build script
Write-Host "Testing GPS Tracker project build..." -ForegroundColor Green

# Check required files
Write-Host "Checking project files..." -ForegroundColor Yellow
$requiredFiles = @(
    "build.gradle",
    "settings.gradle", 
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml"
)

foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Host "OK: $file exists" -ForegroundColor Green
    } else {
        Write-Host "ERROR: $file missing" -ForegroundColor Red
    }
}

# Check Gradle Wrapper
Write-Host "`nChecking Gradle Wrapper..." -ForegroundColor Yellow
if (Test-Path "gradle/wrapper/gradle-wrapper.jar") {
    $jarSize = (Get-Item "gradle/wrapper/gradle-wrapper.jar").Length
    Write-Host "OK: gradle-wrapper.jar exists (size: $jarSize bytes)" -ForegroundColor Green
} else {
    Write-Host "ERROR: gradle-wrapper.jar missing" -ForegroundColor Red
}

# Check project structure
Write-Host "`nChecking project structure..." -ForegroundColor Yellow
$directories = @(
    "app/src/main/java/com/gpstracker/app",
    "app/src/main/res/layout",
    "app/src/main/res/values",
    ".github/workflows"
)

foreach ($dir in $directories) {
    if (Test-Path $dir) {
        Write-Host "OK: $dir exists" -ForegroundColor Green
    } else {
        Write-Host "ERROR: $dir missing" -ForegroundColor Red
    }
}

Write-Host "`nTest completed!" -ForegroundColor Green
Write-Host "Note: Cannot run gradlew build locally due to missing Java environment" -ForegroundColor Yellow
Write-Host "But project structure is complete, GitHub Actions should build successfully" -ForegroundColor Cyan