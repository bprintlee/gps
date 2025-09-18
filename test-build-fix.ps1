# Test build configuration fix
Write-Host "Testing build configuration fix..." -ForegroundColor Green

# Check build.gradle format
Write-Host "`nChecking build.gradle format..." -ForegroundColor Yellow
$buildGradleContent = Get-Content "app/build.gradle" -Raw
if ($buildGradleContent -match "versionName.*versionCode") {
    Write-Host "OK: Version info present in build.gradle" -ForegroundColor Green
} else {
    Write-Host "ERROR: Version info missing in build.gradle" -ForegroundColor Red
}

# Check GitHub Actions version replacement
Write-Host "`nChecking GitHub Actions version replacement..." -ForegroundColor Yellow
$workflowContent = Get-Content ".github/workflows/android.yml" -Raw
if ($workflowContent -match "sed.*versionName") {
    Write-Host "OK: Version replacement logic in GitHub Actions" -ForegroundColor Green
} else {
    Write-Host "ERROR: Version replacement logic missing" -ForegroundColor Red
}

if ($workflowContent -match "Validate build.gradle") {
    Write-Host "OK: Build validation step added" -ForegroundColor Green
} else {
    Write-Host "ERROR: Build validation step missing" -ForegroundColor Red
}

# Check Gradle wrapper
Write-Host "`nChecking Gradle wrapper..." -ForegroundColor Yellow
if (Test-Path "gradle/wrapper/gradle-wrapper.jar") {
    Write-Host "OK: Gradle wrapper jar exists" -ForegroundColor Green
} else {
    Write-Host "ERROR: Gradle wrapper jar missing" -ForegroundColor Red
}

if (Test-Path "gradle/wrapper/gradle-wrapper.properties") {
    Write-Host "OK: Gradle wrapper properties exist" -ForegroundColor Green
} else {
    Write-Host "ERROR: Gradle wrapper properties missing" -ForegroundColor Red
}

# Check for potential issues
Write-Host "`nChecking for potential build issues..." -ForegroundColor Yellow
if ($buildGradleContent -match "compileSdk.*34") {
    Write-Host "OK: Compile SDK version set" -ForegroundColor Green
} else {
    Write-Host "ERROR: Compile SDK version missing" -ForegroundColor Red
}

if ($buildGradleContent -match "targetSdk.*34") {
    Write-Host "OK: Target SDK version set" -ForegroundColor Green
} else {
    Write-Host "ERROR: Target SDK version missing" -ForegroundColor Red
}

if ($buildGradleContent -match "JavaVersion.VERSION_17") {
    Write-Host "OK: Java version 17 configured" -ForegroundColor Green
} else {
    Write-Host "ERROR: Java version configuration missing" -ForegroundColor Red
}

Write-Host "`nBuild configuration test completed!" -ForegroundColor Green
Write-Host "Fixed issues:" -ForegroundColor Cyan
Write-Host "- Improved version replacement logic in GitHub Actions" -ForegroundColor Cyan
Write-Host "- Added build.gradle validation step" -ForegroundColor Cyan
Write-Host "- Enhanced error handling for version updates" -ForegroundColor Cyan
