# PowerShell脚本：创建Android调试keystore
Write-Host "创建Android调试keystore文件..." -ForegroundColor Green

# 检查Java是否安装
try {
    $javaVersion = java -version 2>&1
    Write-Host "Java版本: $javaVersion" -ForegroundColor Yellow
} catch {
    Write-Host "错误: 未找到Java，请先安装Java JDK" -ForegroundColor Red
    Write-Host "下载地址: https://www.oracle.com/java/technologies/downloads/" -ForegroundColor Yellow
    Read-Host "按任意键退出"
    exit 1
}

# 创建keystore
Write-Host "正在创建keystore文件..." -ForegroundColor Yellow
$keystorePath = "debug.keystore"

try {
    keytool -genkey -v -keystore $keystorePath -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
    
    if (Test-Path $keystorePath) {
        Write-Host "✅ Keystore创建成功！" -ForegroundColor Green
        Write-Host "文件位置: keystore\$keystorePath" -ForegroundColor Cyan
        Write-Host "密码: android" -ForegroundColor Cyan
        Write-Host "别名: androiddebugkey" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "现在您可以正常构建和升级安装应用了！" -ForegroundColor Green
    } else {
        Write-Host "❌ Keystore创建失败" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ 创建keystore时出错: $($_.Exception.Message)" -ForegroundColor Red
}

Read-Host "按任意键退出"
