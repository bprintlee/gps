# GPS Tracker 签名配置说明

## 概述

为了确保新版本能够覆盖安装而不需要重新安装，我们配置了统一的签名系统。所有版本（Debug和Release）都使用相同的签名密钥。

## 签名策略

### 1. 统一签名
- **Debug版本**：使用Android默认debug签名
- **Release版本**：使用相同的debug签名（确保覆盖安装）
- **版本更新**：版本号递增，但签名保持不变

### 2. 签名配置

#### 本地开发
```gradle
signingConfigs {
    release {
        storeFile file(System.getProperty("user.home") + "/.android/debug.keystore")
        storePassword "android"
        keyAlias "androiddebugkey"
        keyPassword "android"
    }
}
```

#### GitHub Actions
- 自动创建Android默认debug keystore
- 使用相同的签名参数
- 确保构建的APK具有相同的签名

## 版本管理

### 版本号规则
- **版本名称**：`{基础版本}-build.{时间戳}`
- **版本代码**：自动递增的整数
- **示例**：`1.0.0-build.20241201143022` (版本代码: 2)

### 覆盖安装条件
要成功覆盖安装，需要满足以下条件：
1. ✅ **相同的包名**：`com.gpstracker.app`
2. ✅ **相同的签名**：使用统一的debug签名
3. ✅ **更高的版本代码**：每次构建自动递增
4. ✅ **兼容的targetSdk**：保持API兼容性

## 构建流程

### 1. 版本信息生成
```bash
# 自动生成版本信息
BUILD_TIME=$(date +"%Y%m%d%H%M%S")
VERSION_NAME="${CURRENT_VERSION}-build.${BUILD_TIME}"
VERSION_CODE=$((CURRENT_CODE + 1))
```

### 2. 签名设置
```bash
# 创建debug keystore
keytool -genkey -v -keystore ~/.android/debug.keystore \
  -alias androiddebugkey -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass android -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"
```

### 3. APK构建
```bash
# 构建Debug APK（已签名）
./gradlew assembleDebug

# 构建Release APK（已签名）
./gradlew assembleRelease
```

## 用户安装体验

### 首次安装
1. 下载APK文件
2. 允许安装未知来源应用
3. 正常安装流程

### 版本更新
1. 下载新版本APK
2. **直接覆盖安装**（无需卸载旧版本）
3. 保持所有应用数据和设置

### 安装验证
```bash
# 检查APK签名信息
aapt dump badging app-debug.apk | grep -E "(package|versionCode|versionName)"
```

## 安全考虑

### 使用Debug签名的原因
1. **开发便利性**：无需管理复杂的生产签名
2. **版本兼容性**：确保所有版本都能覆盖安装
3. **测试友好**：便于开发和测试流程

### 生产环境建议
如果将来需要发布到Google Play Store，建议：
1. 创建专用的发布签名密钥
2. 使用Google Play App Signing
3. 保持签名密钥的安全备份

## 故障排除

### 常见问题

#### 1. 安装失败："应用未签名"
**解决方案**：确保使用相同的签名密钥

#### 2. 版本冲突："版本代码已存在"
**解决方案**：检查版本代码是否正确递增

#### 3. 签名不匹配："签名验证失败"
**解决方案**：确保所有构建使用相同的keystore

### 验证命令
```bash
# 检查APK签名
jarsigner -verify -verbose -certs app-debug.apk

# 比较两个APK的签名
keytool -printcert -jarfile app-debug-v1.apk
keytool -printcert -jarfile app-debug-v2.apk
```

## 总结

通过统一的签名配置，我们实现了：
- ✅ **无缝更新**：新版本可以直接覆盖安装
- ✅ **数据保持**：用户数据和应用设置不会丢失
- ✅ **版本管理**：自动递增版本号
- ✅ **构建一致性**：本地和CI环境使用相同签名

这确保了用户在使用GPS Tracker应用时能够获得流畅的更新体验。
