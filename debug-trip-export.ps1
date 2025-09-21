# 调试行程导出问题
Write-Host "=== 行程导出问题调试指南 ===" -ForegroundColor Green

Write-Host "`n问题描述:" -ForegroundColor Yellow
Write-Host "• 导出行程时只能查询到一个行程" -ForegroundColor Red
Write-Host "• 不能选择行程" -ForegroundColor Red

Write-Host "`n可能的原因分析:" -ForegroundColor Yellow
Write-Host "1. 行程管理逻辑问题" -ForegroundColor White
Write-Host "   - 行程没有正确开始或结束" -ForegroundColor Gray
Write-Host "   - 状态切换逻辑有问题" -ForegroundColor Gray

Write-Host "2. 数据库查询问题" -ForegroundColor White
Write-Host "   - trip_id字段为空或null" -ForegroundColor Gray
Write-Host "   - 数据库升级问题" -ForegroundColor Gray

Write-Host "3. 服务连接问题" -ForegroundColor White
Write-Host "   - GPS服务未正确启动" -ForegroundColor Gray
Write-Host "   - 数据未正确保存" -ForegroundColor Gray

Write-Host "`n已添加的调试功能:" -ForegroundColor Green
Write-Host "✓ 数据库查询调试信息" -ForegroundColor Green
Write-Host "✓ 行程管理调试信息" -ForegroundColor Green
Write-Host "✓ GPS数据创建调试信息" -ForegroundColor Green
Write-Host "✓ 导出界面调试信息" -ForegroundColor Green
Write-Host "✓ 改进的行程选择对话框" -ForegroundColor Green

Write-Host "`n调试步骤:" -ForegroundColor Cyan
Write-Host "1. 安装更新后的APK" -ForegroundColor White
Write-Host "2. 启动GPS跟踪服务" -ForegroundColor White
Write-Host "3. 在室内外之间移动几次" -ForegroundColor White
Write-Host "4. 查看日志输出 (adb logcat | grep -E 'GpsTrackingService|GpsDatabase|ExportActivity')" -ForegroundColor White
Write-Host "5. 尝试导出功能" -ForegroundColor White

Write-Host "`n关键日志信息:" -ForegroundColor Yellow
Write-Host "• GpsTrackingService: 开始新行程/结束行程" -ForegroundColor White
Write-Host "• GpsDatabase: 查询到X个行程" -ForegroundColor White
Write-Host "• ExportActivity: 总GPS数据点/查询到行程数量" -ForegroundColor White

Write-Host "`n改进的功能:" -ForegroundColor Green
Write-Host "• 更详细的错误提示" -ForegroundColor White
Write-Host "• 行程编号显示 (行程1, 行程2...)" -ForegroundColor White
Write-Host "• 导出全部行程选项" -ForegroundColor White
Write-Host "• 更好的调试信息" -ForegroundColor White

Write-Host "`n测试建议:" -ForegroundColor Cyan
Write-Host "1. 确保GPS服务正在运行" -ForegroundColor White
Write-Host "2. 在室外移动至少30秒" -ForegroundColor White
Write-Host "3. 回到室内等待状态切换" -ForegroundColor White
Write-Host "4. 重复几次以创建多个行程" -ForegroundColor White
Write-Host "5. 然后尝试导出功能" -ForegroundColor White

Write-Host "`n如果问题仍然存在:" -ForegroundColor Red
Write-Host "• 检查日志中的具体错误信息" -ForegroundColor White
Write-Host "• 确认数据库版本是否正确升级" -ForegroundColor White
Write-Host "• 验证GPS数据是否包含trip_id" -ForegroundColor White

Write-Host "`n调试完成！请按照步骤进行测试。" -ForegroundColor Green
