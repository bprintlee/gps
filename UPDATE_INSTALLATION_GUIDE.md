# GPS Tracker 更新安装指南

## 🎯 解决方案概述

已成功配置统一签名系统，确保新版本能够**覆盖安装**而不需要重新安装，保持所有应用数据和设置。

## ✅ 已实现的功能

### 1. 统一签名配置
- **Debug版本**：使用Android默认debug签名
- **Release版本**：使用相同的debug签名
- **版本更新**：版本号递增，签名保持不变

### 2. 自动版本管理
- **版本名称**：`1.0.0-build.20241201143022`
- **版本代码**：自动递增（1, 2, 3, ...）
- **构建时间**：每次构建自动生成时间戳

### 3. GitHub Actions集成
- 自动创建debug keystore
- 统一签名配置
- 自动构建和发布

## 🔄 用户更新体验

### 首次安装
1. 下载APK文件
2. 允许安装未知来源应用
3. 正常安装流程

### 版本更新（新功能！）
1. 下载新版本APK
2. **直接覆盖安装**（无需卸载旧版本）
3. **保持所有应用数据和设置**
4. 继续使用，无需重新配置

## 📱 安装验证

### 覆盖安装条件
✅ **相同的包名**：`com.gpstracker.app`  
✅ **相同的签名**：使用统一的debug签名  
✅ **更高的版本代码**：每次构建自动递增  
✅ **兼容的targetSdk**：保持API兼容性  

### 验证方法
```bash
# 检查APK签名信息
aapt dump badging app-debug.apk | grep -E "(package|versionCode|versionName)"
```

## 🚀 新功能特性

### GPS行程跟踪功能
- **自动行程管理**：室内/室外状态切换时自动开始/结束行程
- **行程信息显示**：主界面显示当前行程ID和总行程数
- **按行程导出**：每个行程生成独立的GPX文件
- **行程选择导出**：可以查看和选择特定行程进行导出

### 版本更新优势
- **无缝更新**：新版本可以直接覆盖安装
- **数据保持**：用户数据和应用设置不会丢失
- **版本管理**：自动递增版本号
- **构建一致性**：本地和CI环境使用相同签名

## 📋 使用步骤

### 1. 首次安装
1. 访问GitHub Releases页面
2. 下载最新版本的APK文件
3. 在Android设备上安装

### 2. 版本更新
1. 访问GitHub Releases页面
2. 下载新版本APK文件
3. **直接安装**（系统会自动覆盖旧版本）
4. 打开应用，所有数据保持不变

### 3. 功能验证
1. 启动GPS跟踪服务
2. 在室内外之间移动
3. 观察行程自动开始/结束
4. 检查主界面的行程信息显示
5. 使用导出功能查看和导出特定行程

## 🔧 技术实现

### 签名配置
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

### 版本管理
```bash
# 自动生成版本信息
BUILD_TIME=$(date +"%Y%m%d%H%M%S")
VERSION_NAME="${CURRENT_VERSION}-build.${BUILD_TIME}"
VERSION_CODE=$((CURRENT_CODE + 1))
```

## 📊 构建状态

### GitHub Actions
- **状态**：自动构建和发布
- **链接**：https://github.com/bprintlee/gps/actions
- **发布**：https://github.com/bprintlee/gps/releases

### 构建产物
- **Debug APK**：`gps-tracker-debug-{版本代码}.apk`
- **Release APK**：`gps-tracker-release-{版本代码}.apk`

## 🎉 总结

通过配置统一签名系统，我们成功实现了：

1. ✅ **覆盖安装**：新版本可以直接覆盖安装
2. ✅ **数据保持**：用户数据和应用设置不会丢失
3. ✅ **版本管理**：自动递增版本号
4. ✅ **构建一致性**：本地和CI环境使用相同签名
5. ✅ **行程跟踪**：完整的GPS行程管理功能

现在用户可以享受流畅的更新体验，无需担心数据丢失或重新配置应用！

## 📞 支持

如有问题，请访问：
- **GitHub Issues**：https://github.com/bprintlee/gps/issues
- **构建状态**：https://github.com/bprintlee/gps/actions
- **最新发布**：https://github.com/bprintlee/gps/releases
