# 签名配置设置指南

## 问题说明
为了能够通过升级的方式安装应用，需要保持一致的签名。每次构建都必须使用相同的keystore文件。

## 解决方案

### 方法1: 使用Android SDK默认keystore（推荐）

如果您有Android SDK，可以复制默认的调试keystore：

**Windows:**
```cmd
copy "%USERPROFILE%\.android\debug.keystore" "keystore\debug.keystore"
```

**Linux/macOS:**
```bash
cp ~/.android/debug.keystore keystore/debug.keystore
```

### 方法2: 手动创建keystore

如果您有Java环境，可以手动创建：

```bash
keytool -genkey -v -keystore keystore/debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

### 方法3: 使用提供的脚本

运行项目中的脚本：

**Windows:**
```cmd
cd keystore
create_keystore.bat
```

**Linux/macOS:**
```bash
cd keystore
chmod +x create_keystore.sh
./create_keystore.sh
```

## 验证设置

创建keystore后，运行以下命令验证：

```bash
keytool -list -v -keystore keystore/debug.keystore -storepass android
```

应该看到类似输出：
```
别名名称: androiddebugkey
创建日期: [日期]
条目类型: PrivateKeyEntry
证书链长度: 1
证书[1]:
所有者: CN=Android Debug, O=Android, C=US
```

## 重要提醒

1. **保持keystore文件**: 一旦创建，请妥善保存keystore文件
2. **版本控制**: 建议将keystore文件添加到版本控制中（仅用于调试）
3. **生产环境**: 生产环境请使用不同的keystore
4. **升级安装**: 使用相同keystore签名的APK可以正常升级安装

## 故障排除

如果仍然无法升级安装：

1. 确认keystore文件存在且正确
2. 检查build.gradle中的签名配置
3. 卸载旧版本应用，重新安装
4. 检查应用包名是否一致
