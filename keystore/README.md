# Android调试签名配置

## 文件说明

- `debug.properties` - 签名配置文件
- `create_keystore.bat` - Windows批处理脚本，用于创建keystore
- `create_keystore.sh` - Linux/macOS脚本，用于创建keystore
- `debug.keystore` - 调试keystore文件（需要运行脚本生成）

## 使用方法

### 方法1: 运行脚本自动创建

**Windows:**
```bash
cd keystore
create_keystore.bat
```

**Linux/macOS:**
```bash
cd keystore
chmod +x create_keystore.sh
./create_keystore.sh
```

### 方法2: 手动创建

如果您有Java环境，可以手动运行：

```bash
keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
```

### 方法3: 使用Android SDK的默认keystore

如果您有Android SDK，可以复制默认的调试keystore：

**Windows:**
```
copy %USERPROFILE%\.android\debug.keystore keystore\debug.keystore
```

**Linux/macOS:**
```bash
cp ~/.android/debug.keystore keystore/debug.keystore
```

## 签名信息

- **Keystore文件**: `debug.keystore`
- **密码**: `android`
- **别名**: `androiddebugkey`
- **密钥密码**: `android`

## 注意事项

1. 这个keystore仅用于开发和测试
2. 不要用于生产环境发布
3. 保持keystore文件的一致性，确保可以正常升级安装
4. 如果keystore文件丢失，需要重新安装应用（无法升级）

## 集成到项目

创建keystore后，项目会自动使用这个签名配置进行构建，确保每次构建都使用相同的签名，支持应用升级安装。
