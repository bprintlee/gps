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

REM 删除旧的keystore文件（如果存在）
if exist debug.keystore (
    echo 删除旧的keystore文件...
    del debug.keystore
)

REM 创建新的keystore
echo 正在创建新的debug keystore...
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"

if exist debug.keystore (
    echo.
    echo ✅ Keystore创建成功！
    echo 文件位置: debug.keystore
    echo 密码: android
    echo 别名: androiddebugkey
    echo.
    echo 现在您可以正常构建和安装应用了！
) else (
    echo.
    echo ❌ Keystore创建失败
    echo 请检查Java环境是否正确安装
)

pause
