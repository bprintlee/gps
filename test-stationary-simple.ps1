# Test Stationary Logic and Upload to GitHub

Write-Host "=== Testing Stationary Logic Fix ===" -ForegroundColor Green

# Check Git status
Write-Host "Checking Git status..." -ForegroundColor Yellow
git status

# Add modified files
Write-Host "Adding modified files..." -ForegroundColor Yellow
git add app/src/main/java/com/gpstracker/app/TestStationaryLogic.kt
git add app/src/main/java/com/gpstracker/app/TestStationaryActivity.kt
git add app/src/main/res/layout/activity_test_stationary.xml
git add app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/java/com/gpstracker/app/MainActivity.kt

# Commit changes
Write-Host "Committing changes..." -ForegroundColor Yellow
git commit -m "Fix active state to indoor state transition logic

Main fixes:
1. Fixed location history cleanup logic
2. Improved active to indoor state transition detection
3. Added detailed debug logging
4. Created test simulation functionality
5. Added TestStationaryActivity for testing

New features:
- TestStationaryLogic: Simulates indoor state transition logic
- TestStationaryActivity: Test interface for real-time state monitoring
- Added 'Test Switch' button in main interface

Test method:
1. Start app and click 'Test Switch' button
2. Run tests in test interface
3. Check log output for verification"

# Push to GitHub
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git push origin main

Write-Host "=== Test and upload completed ===" -ForegroundColor Green
Write-Host "Please check GitHub Actions build status" -ForegroundColor Cyan
