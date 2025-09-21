@echo off
echo 创建Android调试keystore文件...

REM 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未找到Java，请先安装Java JDK
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

REM 创建keystore
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"

if %errorlevel% equ 0 (
    echo.
    echo ✅ Keystore创建成功！
    echo 文件位置: keystore\debug.keystore
    echo 密码: android
    echo 别名: androiddebugkey
) else (
    echo.
    echo ❌ Keystore创建失败
)

pause
