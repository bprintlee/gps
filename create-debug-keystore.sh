#!/bin/bash

echo "创建Android调试keystore文件..."

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java，请先安装Java JDK"
    echo "Ubuntu/Debian: sudo apt install openjdk-11-jdk"
    echo "macOS: brew install openjdk@11"
    exit 1
fi

# 检查keytool是否可用
if ! command -v keytool &> /dev/null; then
    echo "错误: 未找到keytool，请确保Java JDK正确安装"
    exit 1
fi

# 删除旧的keystore文件（如果存在）
if [ -f "debug.keystore" ]; then
    echo "删除旧的keystore文件..."
    rm debug.keystore
fi

# 创建新的keystore
echo "正在创建新的debug keystore..."
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"

if [ -f "debug.keystore" ]; then
    echo ""
    echo "✅ Keystore创建成功！"
    echo "文件位置: debug.keystore"
    echo "密码: android"
    echo "别名: androiddebugkey"
    echo ""
    echo "现在您可以正常构建和安装应用了！"
else
    echo ""
    echo "❌ Keystore创建失败"
    echo "请检查Java环境是否正确安装"
    exit 1
fi
