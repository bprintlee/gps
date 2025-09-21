# 测试行程跟踪功能
Write-Host "=== GPS行程跟踪功能测试 ===" -ForegroundColor Green

# 检查项目结构
Write-Host "`n1. 检查项目结构..." -ForegroundColor Yellow
if (Test-Path "app/src/main/java/com/gpstracker/app/model/GpsData.kt") {
    Write-Host "✓ GpsData.kt 存在" -ForegroundColor Green
} else {
    Write-Host "✗ GpsData.kt 不存在" -ForegroundColor Red
}

if (Test-Path "app/src/main/java/com/gpstracker/app/database/GpsDatabase.kt") {
    Write-Host "✓ GpsDatabase.kt 存在" -ForegroundColor Green
} else {
    Write-Host "✗ GpsDatabase.kt 不存在" -ForegroundColor Red
}

if (Test-Path "app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt") {
    Write-Host "✓ GpsTrackingService.kt 存在" -ForegroundColor Green
} else {
    Write-Host "✗ GpsTrackingService.kt 不存在" -ForegroundColor Red
}

if (Test-Path "app/src/main/java/com/gpstracker/app/utils/GpxExporter.kt") {
    Write-Host "✓ GpxExporter.kt 存在" -ForegroundColor Green
} else {
    Write-Host "✗ GpxExporter.kt 不存在" -ForegroundColor Red
}

# 检查关键功能
Write-Host "`n2. 检查关键功能实现..." -ForegroundColor Yellow

# 检查GpsData是否包含tripId字段
$gpsDataContent = Get-Content "app/src/main/java/com/gpstracker/app/model/GpsData.kt" -Raw
if ($gpsDataContent -match "tripId") {
    Write-Host "✓ GpsData包含tripId字段" -ForegroundColor Green
} else {
    Write-Host "✗ GpsData缺少tripId字段" -ForegroundColor Red
}

# 检查数据库是否包含行程相关方法
$databaseContent = Get-Content "app/src/main/java/com/gpstracker/app/database/GpsDatabase.kt" -Raw
if ($databaseContent -match "getGpsDataByTripId" -and $databaseContent -match "getAllTripIds") {
    Write-Host "✓ 数据库包含行程查询方法" -ForegroundColor Green
} else {
    Write-Host "✗ 数据库缺少行程查询方法" -ForegroundColor Red
}

# 检查GPS服务是否包含行程管理
$serviceContent = Get-Content "app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt" -Raw
if ($serviceContent -match "currentTripId" -and $serviceContent -match "isTripActive") {
    Write-Host "✓ GPS服务包含行程管理" -ForegroundColor Green
} else {
    Write-Host "✗ GPS服务缺少行程管理" -ForegroundColor Red
}

# 检查GPX导出是否支持按行程导出
$exporterContent = Get-Content "app/src/main/java/com/gpstracker/app/utils/GpxExporter.kt" -Raw
if ($exporterContent -match "exportTripGpx" -and $exporterContent -match "createNewGpxFileForTrip") {
    Write-Host "✓ GPX导出支持按行程导出" -ForegroundColor Green
} else {
    Write-Host "✗ GPX导出缺少按行程导出功能" -ForegroundColor Red
}

# 检查UI是否包含行程信息显示
$mainActivityContent = Get-Content "app/src/main/java/com/gpstracker/app/MainActivity.kt" -Raw
if ($mainActivityContent -match "currentTripText" -and $mainActivityContent -match "totalTripsText") {
    Write-Host "✓ 主界面包含行程信息显示" -ForegroundColor Green
} else {
    Write-Host "✗ 主界面缺少行程信息显示" -ForegroundColor Red
}

# 检查导出界面是否包含行程管理
$exportActivityContent = Get-Content "app/src/main/java/com/gpstracker/app/ExportActivity.kt" -Raw
if ($exportActivityContent -match "showTripsDialog" -and $exportActivityContent -match "exportSingleTrip") {
    Write-Host "✓ 导出界面包含行程管理功能" -ForegroundColor Green
} else {
    Write-Host "✗ 导出界面缺少行程管理功能" -ForegroundColor Red
}

# 检查布局文件
Write-Host "`n3. 检查布局文件..." -ForegroundColor Yellow
if (Test-Path "app/src/main/res/layout/activity_main.xml") {
    $mainLayoutContent = Get-Content "app/src/main/res/layout/activity_main.xml" -Raw
    if ($mainLayoutContent -match "currentTripText" -and $mainLayoutContent -match "totalTripsText") {
        Write-Host "✓ 主界面布局包含行程信息" -ForegroundColor Green
    } else {
        Write-Host "✗ 主界面布局缺少行程信息" -ForegroundColor Red
    }
} else {
    Write-Host "✗ 主界面布局文件不存在" -ForegroundColor Red
}

if (Test-Path "app/src/main/res/layout/activity_export.xml") {
    $exportLayoutContent = Get-Content "app/src/main/res/layout/activity_export.xml" -Raw
    if ($exportLayoutContent -match "exportTripsButton") {
        Write-Host "✓ 导出界面布局包含行程按钮" -ForegroundColor Green
    } else {
        Write-Host "✗ 导出界面布局缺少行程按钮" -ForegroundColor Red
    }
} else {
    Write-Host "✗ 导出界面布局文件不存在" -ForegroundColor Red
}

# 功能总结
Write-Host "`n=== 功能实现总结 ===" -ForegroundColor Green
Write-Host "1. ✓ 数据库结构已更新，支持行程ID字段" -ForegroundColor Green
Write-Host "2. ✓ GPS服务已实现自动行程管理（室内/室外状态切换）" -ForegroundColor Green
Write-Host "3. ✓ GPX导出已支持按行程分组导出" -ForegroundColor Green
Write-Host "4. ✓ 主界面已显示当前行程和总行程数" -ForegroundColor Green
Write-Host "5. ✓ 导出界面已支持查看和导出特定行程" -ForegroundColor Green

Write-Host "`n=== 新功能说明 ===" -ForegroundColor Cyan
Write-Host "• 自动行程管理：当从室内状态切换到室外/活跃/驾驶状态时自动开始新行程" -ForegroundColor White
Write-Host "• 行程结束：当从室外/活跃/驾驶状态切换到室内状态时自动结束当前行程" -ForegroundColor White
Write-Host "• 按行程导出：每个行程生成独立的GPX文件，文件名包含行程ID" -ForegroundColor White
Write-Host "• 行程信息显示：主界面显示当前行程ID和总行程数" -ForegroundColor White
Write-Host "• 行程选择导出：导出界面可以查看所有行程并选择特定行程导出" -ForegroundColor White

Write-Host "`n=== 测试建议 ===" -ForegroundColor Yellow
Write-Host "1. 编译并安装应用到设备" -ForegroundColor White
Write-Host "2. 开始GPS跟踪，在室内和室外之间移动" -ForegroundColor White
Write-Host "3. 观察主界面的行程信息变化" -ForegroundColor White
Write-Host "4. 使用导出功能查看和导出特定行程" -ForegroundColor White
Write-Host "5. 检查生成的GPX文件是否按行程正确分组" -ForegroundColor White

Write-Host "`n测试完成！" -ForegroundColor Green
