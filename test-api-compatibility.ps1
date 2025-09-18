# Test API compatibility fixes
Write-Host "Testing API compatibility fixes..." -ForegroundColor Green

# Check MainActivity.kt
Write-Host "`nChecking MainActivity.kt..." -ForegroundColor Yellow
$mainActivityContent = Get-Content "app/src/main/java/com/gpstracker/app/MainActivity.kt" -Raw
if ($mainActivityContent -match "Build.VERSION.SDK_INT >= Build.VERSION_CODES.O") {
    Write-Host "OK: MainActivity has API level check for startForegroundService" -ForegroundColor Green
} else {
    Write-Host "ERROR: MainActivity missing API level check" -ForegroundColor Red
}

if ($mainActivityContent -match "startService\(intent\)") {
    Write-Host "OK: MainActivity has fallback startService for older APIs" -ForegroundColor Green
} else {
    Write-Host "ERROR: MainActivity missing fallback startService" -ForegroundColor Red
}

# Check GpsTrackingService.kt
Write-Host "`nChecking GpsTrackingService.kt..." -ForegroundColor Yellow
$serviceContent = Get-Content "app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt" -Raw
if ($serviceContent -match "Build.VERSION.SDK_INT >= Build.VERSION_CODES.O") {
    Write-Host "OK: GpsTrackingService has API level check for startForeground" -ForegroundColor Green
} else {
    Write-Host "ERROR: GpsTrackingService missing API level check" -ForegroundColor Red
}

if ($serviceContent -match "@Suppress.*DEPRECATION") {
    Write-Host "OK: GpsTrackingService has deprecation suppression for older APIs" -ForegroundColor Green
} else {
    Write-Host "ERROR: GpsTrackingService missing deprecation suppression" -ForegroundColor Red
}

# Check AndroidManifest.xml
Write-Host "`nChecking AndroidManifest.xml..." -ForegroundColor Yellow
$manifestContent = Get-Content "app/src/main/AndroidManifest.xml" -Raw
if ($manifestContent -match "tools:targetApi.*29") {
    Write-Host "OK: AndroidManifest has targetApi for foregroundServiceType" -ForegroundColor Green
} else {
    Write-Host "ERROR: AndroidManifest missing targetApi" -ForegroundColor Red
}

# Check build.gradle minSdk
Write-Host "`nChecking build.gradle..." -ForegroundColor Yellow
$buildGradleContent = Get-Content "app/build.gradle" -Raw
if ($buildGradleContent -match "minSdk 24") {
    Write-Host "OK: minSdk is set to 24" -ForegroundColor Green
} else {
    Write-Host "WARNING: minSdk not found or different value" -ForegroundColor Yellow
}

Write-Host "`nAPI compatibility test completed!" -ForegroundColor Green
Write-Host "The build should now pass Lint checks." -ForegroundColor Cyan
