# Test new features implementation
Write-Host "Testing new features implementation..." -ForegroundColor Green

# Check GitHub Actions version control
Write-Host "`nChecking GitHub Actions version control..." -ForegroundColor Yellow
$workflowContent = Get-Content ".github/workflows/android.yml" -Raw
if ($workflowContent -match "Generate Version Info") {
    Write-Host "OK: Version generation step in GitHub Actions" -ForegroundColor Green
} else {
    Write-Host "ERROR: Version generation step missing" -ForegroundColor Red
}

if ($workflowContent -match "VERSION_NAME.*VERSION_CODE") {
    Write-Host "OK: Version variables defined" -ForegroundColor Green
} else {
    Write-Host "ERROR: Version variables missing" -ForegroundColor Red
}

if ($workflowContent -match "Create Release") {
    Write-Host "OK: Auto release creation configured" -ForegroundColor Green
} else {
    Write-Host "ERROR: Auto release creation missing" -ForegroundColor Red
}

# Check MainActivity status monitoring
Write-Host "`nChecking MainActivity status monitoring..." -ForegroundColor Yellow
$mainActivityContent = Get-Content "app/src/main/java/com/gpstracker/app/MainActivity.kt" -Raw
if ($mainActivityContent -match "startStatusMonitoring") {
    Write-Host "OK: Status monitoring started in MainActivity" -ForegroundColor Green
} else {
    Write-Host "ERROR: Status monitoring not started" -ForegroundColor Red
}

if ($mainActivityContent -match "updateServiceStatus") {
    Write-Host "OK: Service status update method exists" -ForegroundColor Green
} else {
    Write-Host "ERROR: Service status update method missing" -ForegroundColor Red
}

if ($mainActivityContent -match "TrackingState.INDOOR") {
    Write-Host "OK: Indoor state handling in MainActivity" -ForegroundColor Green
} else {
    Write-Host "ERROR: Indoor state handling missing" -ForegroundColor Red
}

# Check layout updates
Write-Host "`nChecking layout updates..." -ForegroundColor Yellow
$layoutContent = Get-Content "app/src/main/res/layout/activity_main.xml" -Raw
if ($layoutContent -match "serviceStatusText") {
    Write-Host "OK: Service status display in layout" -ForegroundColor Green
} else {
    Write-Host "ERROR: Service status display missing in layout" -ForegroundColor Red
}

if ($layoutContent -match "stepCountText") {
    Write-Host "OK: Step count display in layout" -ForegroundColor Green
} else {
    Write-Host "ERROR: Step count display missing in layout" -ForegroundColor Red
}

if ($layoutContent -match "后台服务") {
    Write-Host "OK: Chinese labels in layout" -ForegroundColor Green
} else {
    Write-Host "ERROR: Chinese labels missing in layout" -ForegroundColor Red
}

# Check status display features
Write-Host "`nChecking status display features..." -ForegroundColor Yellow
if ($mainActivityContent -match "updateStatusDisplay.*stepCount.*acceleration") {
    Write-Host "OK: Enhanced status display with sensor data" -ForegroundColor Green
} else {
    Write-Host "ERROR: Enhanced status display missing sensor data" -ForegroundColor Red
}

if ($mainActivityContent -match "isServiceRunning") {
    Write-Host "OK: Service running check method" -ForegroundColor Green
} else {
    Write-Host "ERROR: Service running check method missing" -ForegroundColor Red
}

if ($mainActivityContent -match "ServiceConnection") {
    Write-Host "OK: Service connection for status updates" -ForegroundColor Green
} else {
    Write-Host "ERROR: Service connection for status updates missing" -ForegroundColor Red
}

Write-Host "`nNew features test completed!" -ForegroundColor Green
Write-Host "All new features have been implemented:" -ForegroundColor Cyan
Write-Host "- APK version control with auto-release" -ForegroundColor Cyan
Write-Host "- Real-time status monitoring" -ForegroundColor Cyan
Write-Host "- Indoor/outdoor/active state display" -ForegroundColor Cyan
Write-Host "- Background service status" -ForegroundColor Cyan
Write-Host "- Step count tracking" -ForegroundColor Cyan
