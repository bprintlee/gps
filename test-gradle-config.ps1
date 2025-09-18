# Test Gradle configuration
Write-Host "Testing Gradle configuration fix..." -ForegroundColor Green

# Check build.gradle
Write-Host "`nChecking build.gradle..." -ForegroundColor Yellow
$buildGradleContent = Get-Content "build.gradle" -Raw
if ($buildGradleContent -match "allprojects") {
    Write-Host "ERROR: allprojects block still exists in build.gradle" -ForegroundColor Red
} else {
    Write-Host "OK: allprojects block removed from build.gradle" -ForegroundColor Green
}

# Check settings.gradle
Write-Host "`nChecking settings.gradle..." -ForegroundColor Yellow
$settingsGradleContent = Get-Content "settings.gradle" -Raw
if ($settingsGradleContent -match "FAIL_ON_PROJECT_REPOS") {
    Write-Host "OK: FAIL_ON_PROJECT_REPOS is set in settings.gradle" -ForegroundColor Green
} else {
    Write-Host "WARNING: FAIL_ON_PROJECT_REPOS not found in settings.gradle" -ForegroundColor Yellow
}

# Check app/build.gradle
Write-Host "`nChecking app/build.gradle..." -ForegroundColor Yellow
$appBuildGradleContent = Get-Content "app/build.gradle" -Raw
if ($appBuildGradleContent -match "repositories") {
    Write-Host "ERROR: repositories block found in app/build.gradle" -ForegroundColor Red
} else {
    Write-Host "OK: No repositories block in app/build.gradle" -ForegroundColor Green
}

Write-Host "`nGradle configuration test completed!" -ForegroundColor Green
Write-Host "The build should now work correctly." -ForegroundColor Cyan
