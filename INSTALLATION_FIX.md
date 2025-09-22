# 安装失败（-7）签名不匹配问题解决方案

## 问题说明
错误代码 -7 表示"与已安装的应用签名不同"，这是因为新构建的APK使用了不同的签名，无法覆盖已安装的应用。

## 解决方案

### 方法1: 卸载旧版本应用（最简单）

**通过手机设置卸载：**
1. 打开手机设置
2. 找到"应用管理"或"应用"
3. 搜索"GPS Tracker"或"GPS跟踪器"
4. 点击应用，选择"卸载"
5. 重新安装新版本APK

**通过ADB卸载（如果有ADB）：**
```bash
adb uninstall com.gpstracker.app
```

### 方法2: 创建一致的签名配置

**步骤1: 创建keystore文件**

**Windows:**
```cmd
create-debug-keystore.bat
```

**Linux/macOS:**
```bash
chmod +x create-debug-keystore.sh
./create-debug-keystore.sh
```

**手动创建（如果有Java环境）：**
```bash
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

**步骤2: 重新构建应用**
```bash
./gradlew clean
./gradlew assembleDebug
```

**步骤3: 安装新APK**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方法3: 使用Android Studio

1. 打开Android Studio
2. 进入 Build → Generate Signed Bundle/APK
3. 选择 "Create new keystore"
4. 保存到项目根目录，命名为 `debug.keystore`
5. 使用以下信息：
   - 密码: `android`
   - 别名: `androiddebugkey`
   - 密钥密码: `android`
6. 重新构建和安装

## 验证签名一致性

**检查APK签名：**
```bash
keytool -printcert -jarfile app-debug.apk
```

**检查已安装应用的签名：**
```bash
adb shell pm dump com.gpstracker.app | grep -A 5 "signatures"
```

## 预防措施

1. **保持keystore文件**: 一旦创建，请妥善保存keystore文件
2. **版本控制**: 将keystore文件添加到版本控制中（仅用于调试）
3. **团队协作**: 团队成员使用相同的keystore文件
4. **备份**: 定期备份keystore文件

## 文件说明

- `debug.keystore` - 项目调试keystore文件
- `create-debug-keystore.bat` - Windows创建脚本
- `create-debug-keystore.sh` - Linux/macOS创建脚本
- `local-signing.gradle` - 本地签名配置示例

## 故障排除

**如果仍然无法安装：**

1. 确认keystore文件存在且正确
2. 检查文件权限
3. 完全卸载旧版本应用
4. 清理构建缓存：`./gradlew clean`
5. 重新构建：`./gradlew assembleDebug`

**常见错误：**
- "Tag number over 30" - keystore格式问题，删除重新创建
- "Invalid keystore format" - keystore文件损坏，重新创建
- "Key not found" - 别名错误，检查别名设置

## 重要提醒

- 这个keystore仅用于开发和测试
- 生产环境请使用不同的keystore
- 保持keystore文件的一致性，确保可以正常升级安装
