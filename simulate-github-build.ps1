# Simulate GitHub Actions build process
Write-Host "Simulating GitHub Actions build process..." -ForegroundColor Green

# Step 1: Generate version info (simulating GitHub Actions)
Write-Host "`nStep 1: Generating version info..." -ForegroundColor Yellow
$BUILD_TIME = Get-Date -Format "yyyyMMddHHmm"
$BUILD_DATE = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$VERSION_NAME = "1.0.0-build.$BUILD_TIME"

# 使用时间戳的后8位作为版本代码，确保在Int32范围内
$VERSION_CODE_RAW = $BUILD_TIME.Substring($BUILD_TIME.Length - 8)
$VERSION_CODE = [int]$VERSION_CODE_RAW.TrimStart('0')
# 如果版本代码为0，设置为1
if ($VERSION_CODE -eq 0) {
    $VERSION_CODE = 1
}
# 确保版本代码不超过Android限制 (2^31-1)
if ($VERSION_CODE -gt 2147483647) {
    $VERSION_CODE = $VERSION_CODE % 1000000000
}

Write-Host "Generated version info:"
Write-Host "Version Name: $VERSION_NAME"
Write-Host "Version Code: $VERSION_CODE"
Write-Host "Build Date: $BUILD_DATE"

# Step 2: Check current build.gradle
Write-Host "`nStep 2: Checking current build.gradle..." -ForegroundColor Yellow
Write-Host "Current version info:"
Select-String -Path "app/build.gradle" -Pattern "(versionName|versionCode)" | ForEach-Object { $_.Line }

# Step 3: Simulate version replacement
Write-Host "`nStep 3: Simulating version replacement..." -ForegroundColor Yellow

# Create a backup
Copy-Item "app/build.gradle" "app/build.gradle.backup"

try {
    # Read the file content
    $content = Get-Content "app/build.gradle" -Raw
    
    # Replace versionName
    $content = $content -replace 'versionName "[^"]*"', "versionName `"$VERSION_NAME`""
    
    # Replace versionCode
    $content = $content -replace 'versionCode \d+', "versionCode $VERSION_CODE"
    
    # Write back to file
    $content | Set-Content "app/build.gradle"
    
    Write-Host "Version replacement completed"
    
    # Show updated content
    Write-Host "Updated version info:"
    Select-String -Path "app/build.gradle" -Pattern "(versionName|versionCode)" | ForEach-Object { $_.Line }
    
    # Step 4: Validate build.gradle
    Write-Host "`nStep 4: Validating build.gradle..." -ForegroundColor Yellow
    
    $validationContent = Get-Content "app/build.gradle" -Raw
    if ($validationContent -match "versionName" -and $validationContent -match "versionCode") {
        Write-Host "Build.gradle validation passed" -ForegroundColor Green
    } else {
        Write-Host "Build.gradle validation failed - missing version info" -ForegroundColor Red
        exit 1
    }
    
    # Check for potential issues
    Write-Host "`nStep 5: Checking for potential issues..." -ForegroundColor Yellow
    
    if ($validationContent -match "versionCode \d+") {
        $versionCodeMatch = [regex]::Match($validationContent, "versionCode (\d+)")
        if ($versionCodeMatch.Success) {
            $versionCodeValue = [int]$versionCodeMatch.Groups[1].Value
            if ($versionCodeValue -gt 0) {
                Write-Host "OK: versionCode is valid: $versionCodeValue" -ForegroundColor Green
            } else {
                Write-Host "ERROR: versionCode is zero or negative: $versionCodeValue" -ForegroundColor Red
            }
        }
    }
    
    if ($validationContent -match 'versionName "[^"]*"') {
        $versionNameMatch = [regex]::Match($validationContent, 'versionName "([^"]*)"')
        if ($versionNameMatch.Success) {
            $versionNameValue = $versionNameMatch.Groups[1].Value
            if ($versionNameValue -ne "") {
                Write-Host "OK: versionName is valid: $versionNameValue" -ForegroundColor Green
            } else {
                Write-Host "ERROR: versionName is empty" -ForegroundColor Red
            }
        }
    }
    
    # Check for null values or syntax issues
    if ($validationContent -match "null") {
        Write-Host "ERROR: Found 'null' value in build.gradle" -ForegroundColor Red
        Select-String -Path "app/build.gradle" -Pattern "null" -Context 2 | ForEach-Object { $_.Line }
    }
    
    # Check line 14 specifically (where the error occurs)
    Write-Host "`nStep 6: Checking line 14 specifically..." -ForegroundColor Yellow
    $lines = Get-Content "app/build.gradle"
    if ($lines.Count -ge 14) {
        Write-Host "Line 14: $($lines[13])" -ForegroundColor Cyan
        if ($lines[13] -match "versionCode") {
            Write-Host "Line 14 contains versionCode - checking for issues..." -ForegroundColor Yellow
            
            # Check if versionCode has proper format
            if ($lines[13] -match "versionCode \d+") {
                Write-Host "OK: Line 14 versionCode format is correct" -ForegroundColor Green
            } else {
                Write-Host "ERROR: Line 14 versionCode format is incorrect" -ForegroundColor Red
            }
        }
    }
    
    Write-Host "`nSimulation completed successfully!" -ForegroundColor Green
    
} catch {
    Write-Host "ERROR during simulation: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    # Restore backup
    if (Test-Path "app/build.gradle.backup") {
        Copy-Item "app/build.gradle.backup" "app/build.gradle"
        Remove-Item "app/build.gradle.backup"
        Write-Host "Restored original build.gradle" -ForegroundColor Yellow
    }
}
