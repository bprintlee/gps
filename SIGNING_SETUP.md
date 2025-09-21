# 签名配置设置指南

## 问题说明
为了能够通过升级的方式安装应用，需要保持一致的签名。每次构建都必须使用相同的keystore文件。

## 快速解决方案

### 方法1: 复制Android SDK默认keystore（最简单）

如果您有Android SDK，直接复制默认的调试keystore：

**Windows:**
```cmd
copy "%USERPROFILE%\.android\debug.keystore" "keystore\debug.keystore"
```

**Linux/macOS:**
```bash
cp ~/.android/debug.keystore keystore/debug.keystore
```

### 方法2: 手动创建keystore

如果您有Java环境，在项目根目录运行：

```bash
keytool -genkey -v -keystore keystore/debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

### 方法3: 使用Android Studio

1. 打开Android Studio
2. 进入 Build → Generate Signed Bundle/APK
3. 选择 "Create new keystore"
4. 保存到 `keystore/debug.keystore`
5. 使用以下信息：
   - 密码: `android`
   - 别名: `androiddebugkey`
   - 密钥密码: `android`

## 验证设置

创建keystore后，运行以下命令验证：

```bash
keytool -list -v -keystore keystore/debug.keystore -storepass android
```

## 项目配置

项目已经配置为自动使用 `keystore/debug.keystore` 文件进行签名。如果文件不存在，会回退到系统默认的debug keystore。

## 重要提醒

1. **保持keystore文件**: 一旦创建，请妥善保存keystore文件
2. **版本控制**: 建议将keystore文件添加到版本控制中（仅用于调试）
3. **升级安装**: 使用相同keystore签名的APK可以正常升级安装
4. **生产环境**: 生产环境请使用不同的keystore

## 故障排除

如果仍然无法升级安装：

1. 确认keystore文件存在：`keystore/debug.keystore`
2. 检查文件权限
3. 卸载旧版本应用，重新安装
4. 检查应用包名是否一致

## 文件结构

```
gps/
├── keystore/
│   ├── debug.keystore          # 调试keystore文件（需要创建）
│   ├── debug.properties        # 签名配置信息
│   ├── create_keystore.bat     # Windows创建脚本
│   ├── create_keystore.sh      # Linux/macOS创建脚本
│   └── README.md               # 详细说明
└── app/
    └── build.gradle            # 已配置签名设置
```

创建keystore文件后，您就可以正常构建和升级安装应用了！
