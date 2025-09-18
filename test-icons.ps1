# Test icon files
Write-Host "Testing icon files..." -ForegroundColor Green

# Check if problematic PNG files are removed
Write-Host "`nChecking for problematic PNG files..." -ForegroundColor Yellow
$problematicFiles = @(
    "app/src/main/res/mipmap-hdpi/ic_launcher.png",
    "app/src/main/res/mipmap-hdpi/ic_launcher_round.png",
    "app/src/main/res/mipmap-mdpi/ic_launcher.png",
    "app/src/main/res/mipmap-mdpi/ic_launcher_round.png",
    "app/src/main/res/mipmap-xhdpi/ic_launcher.png",
    "app/src/main/res/mipmap-xhdpi/ic_launcher_round.png",
    "app/src/main/res/mipmap-xxhdpi/ic_launcher.png",
    "app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png"
)

$allRemoved = $true
foreach ($file in $problematicFiles) {
    if (Test-Path $file) {
        Write-Host "ERROR: $file still exists" -ForegroundColor Red
        $allRemoved = $false
    } else {
        Write-Host "OK: $file removed" -ForegroundColor Green
    }
}

if ($allRemoved) {
    Write-Host "`nAll problematic PNG files have been removed!" -ForegroundColor Green
}

# Check if XML icon files exist
Write-Host "`nChecking XML icon files..." -ForegroundColor Yellow
$xmlIconFiles = @(
    "app/src/main/res/drawable/ic_launcher_foreground.xml",
    "app/src/main/res/drawable/ic_launcher_background.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"
)

foreach ($file in $xmlIconFiles) {
    if (Test-Path $file) {
        Write-Host "OK: $file exists" -ForegroundColor Green
    } else {
        Write-Host "ERROR: $file missing" -ForegroundColor Red
    }
}

# Check icon content
Write-Host "`nChecking icon content..." -ForegroundColor Yellow
$foregroundContent = Get-Content "app/src/main/res/drawable/ic_launcher_foreground.xml" -Raw
if ($foregroundContent -match "GPS定位图标") {
    Write-Host "OK: GPS icon design found in foreground" -ForegroundColor Green
} else {
    Write-Host "WARNING: GPS icon design not found" -ForegroundColor Yellow
}

$backgroundContent = Get-Content "app/src/main/res/drawable/ic_launcher_background.xml" -Raw
if ($backgroundContent -match "渐变背景") {
    Write-Host "OK: Background design found" -ForegroundColor Green
} else {
    Write-Host "WARNING: Background design not found" -ForegroundColor Yellow
}

Write-Host "`nIcon test completed!" -ForegroundColor Green
Write-Host "The build should now work without icon compilation errors." -ForegroundColor Cyan
