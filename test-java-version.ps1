# Test Java version configuration
Write-Host "Testing Java version configuration..." -ForegroundColor Green

# Check GitHub Actions workflow
Write-Host "`nChecking GitHub Actions workflow..." -ForegroundColor Yellow
$workflowContent = Get-Content ".github/workflows/android.yml" -Raw
if ($workflowContent -match "java-version: '17'") {
    Write-Host "OK: GitHub Actions configured for Java 17" -ForegroundColor Green
} else {
    Write-Host "ERROR: GitHub Actions not configured for Java 17" -ForegroundColor Red
}

# Check app/build.gradle
Write-Host "`nChecking app/build.gradle..." -ForegroundColor Yellow
$appBuildContent = Get-Content "app/build.gradle" -Raw
if ($appBuildContent -match "VERSION_17") {
    Write-Host "OK: app/build.gradle configured for Java 17" -ForegroundColor Green
} else {
    Write-Host "ERROR: app/build.gradle not configured for Java 17" -ForegroundColor Red
}

if ($appBuildContent -match "jvmTarget = '17'") {
    Write-Host "OK: Kotlin JVM target set to 17" -ForegroundColor Green
} else {
    Write-Host "ERROR: Kotlin JVM target not set to 17" -ForegroundColor Red
}

# Check Android Gradle Plugin version
Write-Host "`nChecking Android Gradle Plugin version..." -ForegroundColor Yellow
$rootBuildContent = Get-Content "build.gradle" -Raw
if ($rootBuildContent -match "com.android.tools.build:gradle:8.1.2") {
    Write-Host "OK: Android Gradle Plugin 8.1.2 (requires Java 17)" -ForegroundColor Green
} else {
    Write-Host "WARNING: Android Gradle Plugin version not found" -ForegroundColor Yellow
}

Write-Host "`nJava version configuration test completed!" -ForegroundColor Green
Write-Host "The build should now work with Java 17." -ForegroundColor Cyan
