# 测试室内状态切换逻辑并上传到GitHub
# Test Stationary Logic and Upload to GitHub

Write-Host "=== 开始测试室内状态切换逻辑 ===" -ForegroundColor Green

# 检查Git状态
Write-Host "检查Git状态..." -ForegroundColor Yellow
git status

# 添加修改的文件
Write-Host "添加修改的文件..." -ForegroundColor Yellow
git add app/src/main/java/com/gpstracker/app/TestStationaryLogic.kt
git add app/src/main/java/com/gpstracker/app/TestStationaryActivity.kt
git add app/src/main/res/layout/activity_test_stationary.xml
git add app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/java/com/gpstracker/app/MainActivity.kt

# 提交更改
Write-Host "提交更改..." -ForegroundColor Yellow
$commitMessage = @"
修复活跃状态无法切换到室内状态的问题

主要修复内容：
1. 修复位置历史记录清理逻辑问题
2. 改进活跃状态切换到室内状态的检测逻辑
3. 添加详细的调试日志输出
4. 创建测试模拟功能验证逻辑正确性
5. 添加TestStationaryActivity用于测试状态切换

修复的具体问题：
- 位置历史记录清理顺序问题
- 活跃状态切换条件检测不准确
- 缺少详细的调试信息
- 5分钟前位置查找逻辑需要优化

新增功能：
- TestStationaryLogic: 模拟测试室内状态切换逻辑
- TestStationaryActivity: 测试界面，可以实时查看状态和运行测试
- 主界面新增"测试切换"按钮

测试方法：
1. 启动应用，点击"测试切换"按钮
2. 在测试界面点击"运行测试"查看模拟结果
3. 点击"模拟切换"进行实时状态切换测试
4. 查看日志输出确认逻辑正确性
"@

git commit -m $commitMessage

# 推送到GitHub
Write-Host "推送到GitHub..." -ForegroundColor Yellow
git push origin main

Write-Host "=== 测试和上传完成 ===" -ForegroundColor Green
Write-Host "请检查GitHub Actions编译状态" -ForegroundColor Cyan

# 显示修复摘要
Write-Host "`n=== 修复摘要 ===" -ForegroundColor Magenta
Write-Host "1. 修复了位置历史记录清理逻辑" -ForegroundColor White
Write-Host "2. 改进了活跃状态切换到室内状态的检测" -ForegroundColor White
Write-Host "3. 添加了详细的调试日志" -ForegroundColor White
Write-Host "4. 创建了测试模拟功能" -ForegroundColor White
Write-Host "5. 添加了测试界面" -ForegroundColor White

Write-Host "`n=== 测试方法 ===" -ForegroundColor Magenta
Write-Host "1. 启动应用" -ForegroundColor White
Write-Host "2. 点击'测试切换'按钮" -ForegroundColor White
Write-Host "3. 在测试界面运行测试" -ForegroundColor White
Write-Host "4. 查看日志输出确认修复效果" -ForegroundColor White
