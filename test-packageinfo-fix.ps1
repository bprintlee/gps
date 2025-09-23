# Test PackageInfo Fix
# 测试PackageInfo修复

Write-Host "=== Testing PackageInfo Fix ===" -ForegroundColor Green

# Check Git status
Write-Host "Checking Git status..." -ForegroundColor Yellow
git status

# Add modified files
Write-Host "Adding modified files..." -ForegroundColor Yellow
git add app/src/main/java/com/gpstracker/app/GPSTrackerApplication.kt
git add app/src/main/java/com/gpstracker/app/utils/LogManager.kt
git add app/src/main/java/com/gpstracker/app/utils/PackageInfoHelper.kt

# Commit changes
Write-Host "Committing changes..." -ForegroundColor Yellow
git commit -m "Fix PackageInfo null error

Main fixes:
1. Added PackageInfoHelper utility class for safe PackageInfo access
2. Fixed GPSTrackerApplication PackageInfo null handling
3. Fixed LogManager PackageInfo null handling
4. Added comprehensive error handling and logging

PackageInfoHelper features:
- Safe PackageInfo retrieval with null checks
- Version name and code getters
- App installation status check
- Comprehensive error handling
- Detailed logging for debugging

This should resolve the 'packageinfo is null' error by:
- Adding null checks before accessing PackageInfo
- Providing fallback values when PackageInfo is unavailable
- Using try-catch blocks to handle exceptions gracefully
- Centralizing PackageInfo access through a utility class"

# Push to GitHub
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git push origin main

Write-Host "=== PackageInfo fix completed ===" -ForegroundColor Green
Write-Host "Please check GitHub Actions build status" -ForegroundColor Cyan

# Show fix summary
Write-Host "`n=== Fix Summary ===" -ForegroundColor Magenta
Write-Host "1. Created PackageInfoHelper utility class" -ForegroundColor White
Write-Host "2. Added null checks for PackageInfo access" -ForegroundColor White
Write-Host "3. Added comprehensive error handling" -ForegroundColor White
Write-Host "4. Updated GPSTrackerApplication to use safe PackageInfo access" -ForegroundColor White
Write-Host "5. Updated LogManager to use safe PackageInfo access" -ForegroundColor White

Write-Host "`n=== Expected Results ===" -ForegroundColor Magenta
Write-Host "1. No more 'packageinfo is null' errors" -ForegroundColor White
Write-Host "2. Graceful fallback when PackageInfo is unavailable" -ForegroundColor White
Write-Host "3. Better error logging for debugging" -ForegroundColor White
Write-Host "4. More robust application initialization" -ForegroundColor White
