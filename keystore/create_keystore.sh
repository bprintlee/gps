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

# 创建keystore
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore创建成功！"
    echo "文件位置: keystore/debug.keystore"
    echo "密码: android"
    echo "别名: androiddebugkey"
else
    echo ""
    echo "❌ Keystore创建失败"
    exit 1
fi
