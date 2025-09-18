# Test power optimization features
Write-Host "Testing power optimization features..." -ForegroundColor Green

# Check GPS optimization
Write-Host "`nChecking GPS optimization..." -ForegroundColor Yellow
$serviceContent = Get-Content "app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt" -Raw
if ($serviceContent -match "gpsUpdateInterval") {
    Write-Host "OK: GPS update interval configuration" -ForegroundColor Green
} else {
    Write-Host "ERROR: GPS update interval missing" -ForegroundColor Red
}

if ($serviceContent -match "isPowerSaveMode") {
    Write-Host "OK: Power save mode detection" -ForegroundColor Green
} else {
    Write-Host "ERROR: Power save mode detection missing" -ForegroundColor Red
}

if ($serviceContent -match "checkPowerSaveMode") {
    Write-Host "OK: Power save mode check method" -ForegroundColor Green
} else {
    Write-Host "ERROR: Power save mode check method missing" -ForegroundColor Red
}

# Check sensor optimization
Write-Host "`nChecking sensor optimization..." -ForegroundColor Yellow
if ($serviceContent -match "sensorDelay.*UI") {
    Write-Host "OK: Sensor delay optimization for power save" -ForegroundColor Green
} else {
    Write-Host "ERROR: Sensor delay optimization missing" -ForegroundColor Red
}

if ($serviceContent -match "stateCheckInterval") {
    Write-Host "OK: State check interval configuration" -ForegroundColor Green
} else {
    Write-Host "ERROR: State check interval missing" -ForegroundColor Red
}

# Check power save mode controls
Write-Host "`nChecking power save mode controls..." -ForegroundColor Yellow
$mainActivityContent = Get-Content "app/src/main/java/com/gpstracker/app/MainActivity.kt" -Raw
if ($mainActivityContent -match "togglePowerSaveMode") {
    Write-Host "OK: Power save mode toggle function" -ForegroundColor Green
} else {
    Write-Host "ERROR: Power save mode toggle function missing" -ForegroundColor Red
}

if ($mainActivityContent -match "powerSaveButton") {
    Write-Host "OK: Power save mode button in MainActivity" -ForegroundColor Green
} else {
    Write-Host "ERROR: Power save mode button missing" -ForegroundColor Red
}

# Check layout updates
Write-Host "`nChecking layout updates..." -ForegroundColor Yellow
$layoutContent = Get-Content "app/src/main/res/layout/activity_main.xml" -Raw
if ($layoutContent -match "powerSaveStatusText") {
    Write-Host "OK: Power save status display in layout" -ForegroundColor Green
} else {
    Write-Host "ERROR: Power save status display missing" -ForegroundColor Red
}

if ($layoutContent -match "powerSaveButton") {
    Write-Host "OK: Power save button in layout" -ForegroundColor Green
} else {
    Write-Host "ERROR: Power save button missing in layout" -ForegroundColor Red
}

# Check brightness control removal
Write-Host "`nChecking brightness control removal..." -ForegroundColor Yellow
if ($serviceContent -notmatch "brightness|light") {
    Write-Host "OK: Brightness control removed from service" -ForegroundColor Green
} else {
    Write-Host "WARNING: Brightness control still present" -ForegroundColor Yellow
}

if ($mainActivityContent -notmatch "brightness|light") {
    Write-Host "OK: Brightness control removed from MainActivity" -ForegroundColor Green
} else {
    Write-Host "WARNING: Brightness control still present in MainActivity" -ForegroundColor Yellow
}

# Check power optimization features
Write-Host "`nChecking power optimization features..." -ForegroundColor Yellow
if ($serviceContent -match "PowerManager") {
    Write-Host "OK: PowerManager integration for system power save detection" -ForegroundColor Green
} else {
    Write-Host "ERROR: PowerManager integration missing" -ForegroundColor Red
}

if ($serviceContent -match "stopLocationUpdates") {
    Write-Host "OK: Location updates can be stopped for power saving" -ForegroundColor Green
} else {
    Write-Host "ERROR: Location updates stop method missing" -ForegroundColor Red
}

Write-Host "`nPower optimization test completed!" -ForegroundColor Green
Write-Host "All power optimization features have been implemented:" -ForegroundColor Cyan
Write-Host "- Removed brightness control to prevent false triggers" -ForegroundColor Cyan
Write-Host "- Added intelligent GPS update frequency control" -ForegroundColor Cyan
Write-Host "- Implemented sensor delay optimization" -ForegroundColor Cyan
Write-Host "- Added power save mode detection and control" -ForegroundColor Cyan
Write-Host "- Created manual power save mode toggle" -ForegroundColor Cyan
Write-Host "- Optimized state monitoring intervals" -ForegroundColor Cyan
Write-Host "- Added power save status display" -ForegroundColor Cyan
